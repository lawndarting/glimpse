/*
 * Copyright (c) 2012, Metron, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Metron, Inc. nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL METRON, INC. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.metsci.glimpse.plot.timeline;

import static com.metsci.glimpse.plot.stacked.StackedPlot2D.Orientation.HORIZONTAL;
import static com.metsci.glimpse.plot.stacked.StackedPlot2D.Orientation.VERTICAL;
import static com.metsci.glimpse.support.font.FontUtils.getDefaultBold;
import static com.metsci.glimpse.support.font.FontUtils.getDefaultPlain;

import java.awt.Font;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.metsci.glimpse.axis.Axis1D;
import com.metsci.glimpse.axis.listener.mouse.AxisMouseListener;
import com.metsci.glimpse.axis.listener.mouse.AxisMouseListener1D;
import com.metsci.glimpse.axis.painter.NumericXYAxisPainter;
import com.metsci.glimpse.axis.painter.TimeAxisPainter;
import com.metsci.glimpse.axis.painter.TimeXAxisPainter;
import com.metsci.glimpse.axis.painter.TimeYAxisPainter;
import com.metsci.glimpse.axis.painter.label.AxisLabelHandler;
import com.metsci.glimpse.axis.painter.label.GridAxisLabelHandler;
import com.metsci.glimpse.axis.tagged.Constraint;
import com.metsci.glimpse.axis.tagged.Tag;
import com.metsci.glimpse.axis.tagged.TaggedAxis1D;
import com.metsci.glimpse.context.GlimpseTargetStack;
import com.metsci.glimpse.event.mouse.GlimpseMouseEvent;
import com.metsci.glimpse.event.mouse.GlimpseMouseMotionListener;
import com.metsci.glimpse.layout.GlimpseAxisLayout1D;
import com.metsci.glimpse.layout.GlimpseAxisLayout2D;
import com.metsci.glimpse.layout.GlimpseAxisLayoutX;
import com.metsci.glimpse.layout.GlimpseAxisLayoutY;
import com.metsci.glimpse.layout.GlimpseLayout;
import com.metsci.glimpse.painter.decoration.BackgroundPainter;
import com.metsci.glimpse.painter.decoration.BorderPainter;
import com.metsci.glimpse.painter.decoration.GridPainter;
import com.metsci.glimpse.painter.group.DelegatePainter;
import com.metsci.glimpse.painter.info.SimpleTextPainter;
import com.metsci.glimpse.painter.info.SimpleTextPainter.HorizontalPosition;
import com.metsci.glimpse.painter.info.SimpleTextPainter.VerticalPosition;
import com.metsci.glimpse.painter.info.TooltipPainter;
import com.metsci.glimpse.plot.stacked.PlotInfo;
import com.metsci.glimpse.plot.stacked.PlotInfoWrapper;
import com.metsci.glimpse.plot.stacked.StackedPlot2D;
import com.metsci.glimpse.plot.timeline.data.Epoch;
import com.metsci.glimpse.plot.timeline.event.EventPlotInfo;
import com.metsci.glimpse.plot.timeline.layout.TimePlotInfo;
import com.metsci.glimpse.plot.timeline.layout.TimePlotInfoImpl;
import com.metsci.glimpse.plot.timeline.listener.DataAxisMouseListener1D;
import com.metsci.glimpse.plot.timeline.listener.TimeAxisMouseListener1D;
import com.metsci.glimpse.plot.timeline.painter.SelectedTimeRegionPainter;
import com.metsci.glimpse.support.atlas.TextureAtlas;
import com.metsci.glimpse.support.color.GlimpseColor;
import com.metsci.glimpse.support.font.FontUtils;
import com.metsci.glimpse.util.units.time.Time;
import com.metsci.glimpse.util.units.time.TimeStamp;

/**
 * A {@link StackedPlot2D} which automatically creates a timeline axis at the
 * bottom of the stack and uses a
 * {@link com.metsci.glimpse.axis.tagged.TaggedAxis1D} to define a selected time
 * region.
 *
 * @author ulman
 */
public class StackedTimePlot2D extends StackedPlot2D
{
    public static final String MIN_TIME = "min_time";
    public static final String MAX_TIME = "max_time";
    public static final String CURRENT_TIME = "current_time";

    public static final String BACKGROUND = "Timeline Background";
    public static final String TIMELINE = "Timeline";

    // tags representing the minimum and maximum bounds of the selected time window
    protected Tag minTag;
    protected Tag maxTag;
    // tag representing the currently selected time
    protected Tag currentTag;

    // epoch encapsulating the absolute time which maps to value 0 on the timeline
    protected Epoch epoch;

    // time layout painters and listeners
    protected PlotInfo timelineInfo;
    protected GlimpseAxisLayout1D timeLayout;
    protected DelegatePainter timeAxisDelegate;
    protected TimeAxisPainter timeAxisPainter;
    protected PlotInfo selectedLayout;

    // timeline painters and listeners
    protected AxisMouseListener1D timelineMouseListener;
    protected SimpleTextPainter timeUnitsPainter;
    protected BorderPainter timeAxisBorderPainter;
    protected SelectedTimeRegionPainter selectedTimePainter;

    protected TooltipPainter tooltipPainter;

    // default settings for TimelineMouseListeners of new plots
    protected boolean allowPanX = true;
    protected boolean allowPanY = true;
    protected boolean allowZoomX = true;
    protected boolean allowZoomY = true;
    protected boolean allowSelectionLock = true;
    protected boolean currentTimeLock;

    // the size of the label layout area in pixels
    protected int labelLayoutSize;
    protected boolean showLabelLayout = false;
    
    protected boolean showTimeline = true;

    protected TextureAtlas defaultTextureAtlas;

    public StackedTimePlot2D( )
    {
        this( Orientation.VERTICAL, Epoch.posixEpoch( ) );
    }

    /**
     * Creates a vertical StackedTimePlot2D. The provided epoch determines what
     * absolute timestamp corresponds to value 0.0 on the time Axis1D.
     */
    public StackedTimePlot2D( Epoch epoch )
    {
        this( Orientation.VERTICAL, epoch );
    }

    public StackedTimePlot2D( Orientation orientation )
    {
        this( orientation, Epoch.posixEpoch( ) );
    }

    public StackedTimePlot2D( Orientation orientation, Epoch epoch )
    {
        this( orientation, epoch, new TextureAtlas( ) );
    }

    public StackedTimePlot2D( Orientation orientation, Epoch epoch, TextureAtlas atlas )
    {
        this( orientation, epoch, atlas, null );
    }
    
    public StackedTimePlot2D( Orientation orientation, Epoch epoch, TaggedAxis1D commonAxis )
    {
        this( orientation, epoch, new TextureAtlas( ), commonAxis );
    }
    
    /**
     * Creates a StackedTimePlot2D with specified orientation. The provided
     * epoch determines what absolute timestamp corresponds to value 0.0 on the
     * time Axis1D.
     */
    public StackedTimePlot2D( Orientation orientation, Epoch epoch, TextureAtlas atlas, TaggedAxis1D commonAxis )
    {
        super( orientation, commonAxis );

        this.epoch = epoch;
        this.defaultTextureAtlas = atlas;

        this.initializeTimePlot( );
        this.initializeOverlayPainters( );
    }
    
    public void setShowTimeline( boolean showTimeline )
    {
        this.showTimeline = showTimeline;
        this.timelineInfo.getLayout( ).setVisible( showTimeline );
        
        if ( this.isAutoValidate( ) ) this.validate( );
    }
    
    public boolean isShowTimeline( )
    {
        return this.showTimeline;
    }

    @Override
    protected int getOverlayLayoutOffsetX( )
    {
        return orient == VERTICAL ? labelLayoutSize : 0;
    }

    @Override
    protected int getOverlayLayoutOffsetY2( )
    {
        return orient == VERTICAL ? 0 : -labelLayoutSize;
    }

    @Override
    public void setPlotSpacing( int size )
    {
        this.plotSpacing = size;

        for ( PlotInfo info : stackedPlots.values( ) )
        {
            // don't automatically change the timeline info plot spacing
            if ( info.equals( timelineInfo ) ) continue;

            info.setPlotSpacing( size );
        }

        if ( this.isAutoValidate( ) ) this.validate( );
    }

    /**
     * StackedTimePlot2D provides a GlimpseAxisLayout1D which stretches over
     * all the underlying plots and timeline. By default, it passes through mouse
     * events to the underlying GlimpseLayouts and is used to display the blue
     * time selection interval box.
     * 
     * However, it can be used to perform arbitrary drawing on the timeline which
     * must stretch across multiple plots.
     */
    public GlimpseAxisLayout1D getOverlayLayout( )
    {
        return this.overlayLayout;
    }

    public GlimpseAxisLayout1D getUnderlayLayout( )
    {
        return this.underlayLayout;
    }

    /**
     * The layout on which the time axis markings are painted. Painters may be added
     * to this layout to draw additional decorations onto the time axis.
     */
    public GlimpseAxisLayout1D getTimelineLayout( )
    {
        return this.timeLayout;
    }

    public TooltipPainter getTooltipPainter( )
    {
        return this.tooltipPainter;
    }

    /**
     * <p>Returns only the TimePlotInfo handles for plotting areas created with 
     * {@link #createTimePlot(String)}.</p>
     * 
     * Note, this may not be all the plotting areas for this StackedPlot2D if some
     * vanilla plots were created using {@link #createPlot(String)}.
     */
    public Collection<TimePlotInfo> getAllTimePlots( )
    {
        this.lock.lock( );
        try
        {
            List<TimePlotInfo> list = new LinkedList<TimePlotInfo>( );

            for ( PlotInfo plot : getAllPlots( ) )
            {
                if ( plot instanceof TimePlotInfo ) list.add( ( TimePlotInfo ) plot );
            }

            return list;
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    /**
     * Returns the time plot handle for the plot identified via its unique string identifier.
     * 
     * @param id a plot unique identifier
     * @return the TimePlotInfo handle
     */
    public TimePlotInfo getTimePlot( Object id )
    {
        this.lock.lock( );
        try
        {
            PlotInfo plot = getPlot( id );

            if ( plot instanceof TimePlotInfo )
            {
                return ( TimePlotInfo ) plot;
            }
            else
            {
                return null;
            }
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    /**
     * Returns the event plot handle for the plot identified via its unique string identifier.
     * 
     * @param id a plot unique identifier
     * @return the EventPlotInfo handle
     */
    public EventPlotInfo getEventPlot( Object id )
    {
        this.lock.lock( );
        try
        {
            PlotInfo plot = getPlot( id );

            if ( plot instanceof EventPlotInfo )
            {
                return ( EventPlotInfo ) plot;
            }
            else
            {
                return null;
            }
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    public void setSelectedPlot( Object id )
    {
        this.setSelectedPlot( getPlot( id ) );
    }

    public void setSelectedPlot( PlotInfo layout )
    {
        this.selectedLayout = layout;
    }

    public PlotInfo getSelectedPlot( )
    {
        return this.selectedLayout;
    }

    public void setTimeAxisMouseListener( AxisMouseListener1D listener )
    {
        this.lock.lock( );
        try
        {
            if ( this.timelineMouseListener != null )
            {
                this.underlayLayout.removeGlimpseMouseAllListener( this.timelineMouseListener );
            }

            if ( listener != null )
            {
                this.underlayLayout.addGlimpseMouseAllListener( listener );
            }

            this.timelineMouseListener = listener;
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    public AxisMouseListener1D getTimeAxisMouseListener( )
    {
        return this.timelineMouseListener;
    }

    /**
     * Sets whether or not locking of the selected region is allowed for all
     * timeline and plot axes. This setting will also affect newly created plots.
     * 
     * @param lock whether to allow locking of the selected region
     * @see AxisMouseListener#setAllowSelectionLock(boolean)
     */
    public void setAllowSelectionLock( boolean lock )
    {
        this.lock.lock( );
        try
        {
            this.allowSelectionLock = lock;

            this.timelineMouseListener.setAllowSelectionLock( lock );

            for ( TimePlotInfo info : getAllTimePlots( ) )
            {
                info.getDataAxisMouseListener( ).setAllowSelectionLock( lock );
            }
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    /**
     * Sets whether or not zooming of the Y axis is allowed for all
     * timeline and plot axes. This setting will also affect newly created plots.
     * 
     * @param lock whether to allow zooming of the Y axis
     * @see AxisMouseListener#setAllowZoomY(boolean)
     */
    public void setAllowZoomY( boolean lock )
    {
        this.lock.lock( );
        try
        {
            this.allowZoomY = lock;

            if ( this.getOrientation( ) == Orientation.HORIZONTAL )
            {
                this.timelineMouseListener.setAllowZoom( lock );
            }
            else
            {
                for ( TimePlotInfo info : getAllTimePlots( ) )
                {
                    info.getDataAxisMouseListener( ).setAllowZoom( lock );
                }
            }
        }
        finally
        {
            this.lock.unlock( );
        }

    }

    /**
     * Sets whether or not zooming of the X axis is allowed for all
     * timeline and plot axes. This setting will also affect newly created plots.
     * 
     * @param lock whether to allow zooming of the X axis
     * @see AxisMouseListener#setAllowZoomX(boolean)
     */
    public void setAllowZoomX( boolean lock )
    {
        this.lock.lock( );
        try
        {
            this.allowZoomX = lock;

            if ( this.getOrientation( ) == Orientation.VERTICAL )
            {
                this.timelineMouseListener.setAllowZoom( lock );
            }
            else
            {
                for ( TimePlotInfo info : getAllTimePlots( ) )
                {
                    info.getDataAxisMouseListener( ).setAllowZoom( lock );
                }
            }
        }
        finally
        {
            this.lock.unlock( );
        }

    }

    /**
     * Sets whether or not panning of the Y axis is allowed for all
     * timeline and plot axes. This setting will also affect newly created plots.
     * 
     * @param lock whether to allow panning of the Y axis
     * @see AxisMouseListener#setAllowPanY(boolean)
     */
    public void setAllowPanY( boolean lock )
    {
        this.lock.lock( );
        try
        {
            this.allowPanY = lock;

            if ( this.getOrientation( ) == Orientation.HORIZONTAL )
            {
                this.timelineMouseListener.setAllowPan( lock );
            }
            else
            {
                for ( TimePlotInfo info : getAllTimePlots( ) )
                {
                    info.getDataAxisMouseListener( ).setAllowPan( lock );
                }
            }
        }
        finally
        {
            this.lock.unlock( );
        }

    }

    /**
     * Sets whether or not panning of the X axis is allowed for all
     * timeline and plot axes. This setting will also affect newly created plots.
     * 
     * @param lock whether to allow panning of the X axis
     * @see AxisMouseListener#setAllowPanX(boolean)
     */
    public void setAllowPanX( boolean lock )
    {
        this.lock.lock( );
        try
        {
            this.allowPanX = lock;

            if ( this.getOrientation( ) == Orientation.VERTICAL )
            {
                this.timelineMouseListener.setAllowPan( lock );
            }
            else
            {
                for ( TimePlotInfo info : getAllTimePlots( ) )
                {
                    info.getDataAxisMouseListener( ).setAllowPan( lock );
                }
            }
        }
        finally
        {
            this.lock.unlock( );
        }

    }

    public void setTimeAxisPainter( TimeAxisPainter painter )
    {
        this.lock.lock( );
        try
        {
            this.timeAxisDelegate.removePainter( this.timeAxisPainter );
            this.timeAxisPainter = painter;
            this.timeAxisDelegate.addPainter( this.timeAxisPainter );
        }
        finally
        {
            this.lock.unlock( );
        }

    }

    /**
     * Get the TaggedAxis1D Tag which defines the currently selected time.
     *
     * @return the current time selection Tag
     */
    public Tag getTimeSelectionTag( )
    {
        return this.currentTag;
    }

    /**
     * Get the TaggedAxis1D Tag which defines the earliest endpoint of the
     * selected time region.
     *
     * @return the earliest time selection Tag
     */
    public Tag getTimeSelectionMinTag( )
    {
        return this.minTag;
    }

    /**
     * Get the TaggedAxis1D Tag which defines the latest endpoint of the
     * selected time region.
     *
     * @return the latest time selection Tag
     */
    public Tag getTimeSelectionMaxTag( )
    {
        return this.maxTag;
    }

    /**
     * Get the currently selected time (usually equal to getTimeSelectionMax()).
     */
    public TimeStamp getTimeSelection( )
    {
        return epoch.toTimeStamp( currentTag.getValue( ) );
    }

    /**
     * Get the TimeStamp of earliest endpoint of the selected time region.
     */
    public TimeStamp getTimeSelectionMin( )
    {
        return epoch.toTimeStamp( minTag.getValue( ) );
    }

    /**
     * Get the TimeStamp of latest endpoint of the selected time region.
     */
    public TimeStamp getTimeSelectionMax( )
    {
        return epoch.toTimeStamp( maxTag.getValue( ) );
    }

    public Epoch getEpoch( )
    {
        return this.epoch;
    }

    public void setEpoch( Epoch epoch )
    {
        this.epoch = epoch;
        this.timeAxisPainter.setEpoch( epoch );
    }

    public TimeStamp toTimeStamp( double value )
    {
        return epoch.toTimeStamp( value );
    }

    public double fromTimeStamp( TimeStamp value )
    {
        return epoch.fromTimeStamp( value );
    }

    public TaggedAxis1D getTimeAxis( )
    {
        return ( TaggedAxis1D ) this.commonAxis;
    }

    public TimeAxisPainter getTimeAxisPainter( )
    {
        return this.timeAxisPainter;
    }

    public SimpleTextPainter getTimeUnitsPainter( )
    {
        return this.timeUnitsPainter;
    }

    public BorderPainter getTimeAxisBorderPainter( )
    {
        return this.timeAxisBorderPainter;
    }

    public SelectedTimeRegionPainter getSelectedTimePainter( )
    {
        return this.selectedTimePainter;
    }

    public PlotInfo getTimelinePlotInfo( )
    {
        return this.timelineInfo;
    }

    public void setAxisColor( float[] rgba )
    {
        this.timeAxisPainter.setTextColor( rgba );
        this.timeAxisPainter.setTickColor( rgba );
    }

    public void setAxisFont( Font font )
    {
        this.timeAxisPainter.setFont( font );
    }

    public void setShowCurrentTime( boolean show )
    {
        this.timeAxisPainter.showCurrentTimeLabel( show );
    }

    public void setCurrentTimeColor( float[] rgba )
    {
        this.timeAxisPainter.setCurrentTimeTextColor( rgba );
        this.timeAxisPainter.setCurrentTimeTickColor( rgba );
    }

    public void setLabelSize( int size )
    {
        this.labelLayoutSize = size;
        this.validateLayout( );
    }

    public void showLabels( boolean show )
    {
        this.showLabelLayout = show;
        this.validateLayout( );
    }

    public int getLabelSize( )
    {
        return this.labelLayoutSize;
    }

    public boolean isShowLabels( )
    {
        return this.showLabelLayout;
    }

    public boolean isTimeAxisHorizontal( )
    {
        return getOrientation( ) == Orientation.VERTICAL;
    }

    /**
     * Pushes the layout stack for the named plot onto the provided
     * GlimpseTargetStack.
     *
     * @param id unique identifier for the plot
     * @return a relative GlimpseTargetStack for the named plot
     */
    public GlimpseTargetStack pushLayoutTargetStack( GlimpseTargetStack stack, Object id )
    {
        stack = pushPlotTargetStack( stack );
        PlotInfo plot = getPlot( id );
        stack.push( plot.getLayout( ) );
        return stack;
    }

    /**
     * Pushes the layout stack for the base layout of this StackedTimePlot2D
     * onto the provided GlimpseTargetStack.
     *
     * @return a relative GlimpseTargetStack for the timeline plot background
     *         layout
     */
    public GlimpseTargetStack pushPlotTargetStack( GlimpseTargetStack stack )
    {
        stack.push( this );
        return stack;
    }

    /**
     * @see #createPlot(Object )
     */
    public TimePlotInfo createTimePlot( )
    {
        return createTimePlot( UUID.randomUUID( ) );
    }

    /**
     * @see #createPlot(Object, Axis1D )
     */
    public TimePlotInfo createTimePlot( Object id )
    {
        return createTimePlot( id, new Axis1D( ) );
    }

    /**
     * Creates a plot similar to {@code createPlot( String, Axis1D )} but with
     * additional plot decorations, including: grid lines, axes labels for the
     * data axis, and a text label describing the plot.
     *
     * @see #createPlot(Object, Axis1D )
     */
    public TimePlotInfo createTimePlot( Object id, Axis1D axis )
    {
        this.lock.lock( );
        try
        {
            PlotInfo plotInfo = createPlot0( id, axis );
            TimePlotInfo timePlotInfo = createTimePlot0( plotInfo );
            stackedPlots.put( id, timePlotInfo );
            
            if ( isAutoValidate( ) ) validate( );
            
            return timePlotInfo;
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    public TextureAtlas getTextureAtlas( )
    {
        return this.defaultTextureAtlas;
    }

    public EventPlotInfo createEventPlot( )
    {
        return createEventPlot( UUID.randomUUID( ) );
    }

    public EventPlotInfo createEventPlot( Object id )
    {
        return createEventPlot( id, defaultTextureAtlas );
    }

    protected EventPlotInfo createEventPlot( Object id, TextureAtlas atlas )
    {
        if ( !isTimeAxisHorizontal( ) )
        {
            throw new UnsupportedOperationException( "Event Plots are currently not supported by HORIZTONAL StackedTimePlot2D" );
        }

        this.lock.lock( );
        try
        {
            PlotInfo plotInfo = createPlot0( id, new Axis1D( ) );
            EventPlotInfo timePlotInfo = createEventPlot0( plotInfo, atlas );
            stackedPlots.put( id, timePlotInfo );
            
            if ( isAutoValidate( ) ) validate( );
            
            return timePlotInfo;
        }
        finally
        {
            this.lock.unlock( );
        }
    }

    public boolean isLocked( )
    {
        return isSelectionLocked( ) || isCurrentTimeLocked( );
    }

    public boolean isSelectionLocked( )
    {
        return getTimeAxis( ).isSelectionLocked( );
    }

    /**
     * Fixes the selected time region so that it will no longer follow the mouse
     * cursor.
     *
     * @param lock whether to lock or unlock the selected time region
     */
    public void setSelectionLocked( boolean lock )
    {
        getTimeAxis( ).setSelectionLock( lock );
        getTimeAxis( ).validate( );
    }

    public boolean isCurrentTimeLocked( )
    {
        return currentTimeLock;
    }

    /**
     * Fixes the selected time region and the timeline bounds with the current
     * maximum of the selected time region at the far right of the timeline.
     *
     * @param lock
     */
    public void setCurrentTimeLocked( boolean lock )
    {
        currentTimeLock = lock;

        if ( lock )
        {
            double maxValue = maxTag.getValue( );

            TimeStamp maxTime = epoch.toTimeStamp( maxValue );

            shiftTimeSelection( maxTime );
            shiftTimeAxisBounds( maxTime );
            getTimeAxis( ).lockMax( maxValue );
        }
        else
        {
            getTimeAxis( ).unlockMax( );
        }

        getTimeAxis( ).validate( );
    }

    public void setTimeSelection( TimeStamp minTime, TimeStamp selectedTime, TimeStamp maxTime )
    {
        minTag.setValue( epoch.fromTimeStamp( minTime ) );
        maxTag.setValue( epoch.fromTimeStamp( maxTime ) );
        currentTag.setValue( epoch.fromTimeStamp( selectedTime ) );

        TaggedAxis1D axis = getTimeAxis( );
        axis.validateTags( );
        axis.validate( );

        if ( isCurrentTimeLocked( ) )
        {
            shiftTimeAxisBounds( maxTime );
        }
    }

    public void setTimeAxisBounds( TimeStamp minTime, TimeStamp maxTime )
    {
        TaggedAxis1D axis = getTimeAxis( );

        axis.setMax( epoch.fromTimeStamp( maxTime ) );
        axis.setMin( epoch.fromTimeStamp( minTime ) );

        axis.validate( );

        if ( isCurrentTimeLocked( ) )
        {
            shiftTimeSelection( maxTime );
        }
    }

    public void shiftTimeAxisBounds( TimeStamp maxTime )
    {
        TaggedAxis1D axis = getTimeAxis( );

        double diff = axis.getMax( ) - axis.getMin( );
        double max = epoch.fromTimeStamp( maxTime );

        axis.setMax( max );
        axis.setMin( max - diff );

        axis.validate( );
    }

    public void setTimeSelection( TimeStamp minTime, TimeStamp maxTime )
    {
        setTimeSelection( minTime, maxTime, maxTime );
    }

    public void shiftTimeSelection( TimeStamp maxTime )
    {
        double diff = Time.fromSeconds( maxTag.getValue( ) - minTag.getValue( ) );
        TimeStamp minTime = maxTime.subtract( diff );
        setTimeSelection( minTime, maxTime, maxTime );
    }

    protected void initializeOverlayPainters( )
    {
        this.selectedTimePainter = new SelectedTimeRegionPainter( this );

        this.tooltipPainter = new TooltipPainter( this.defaultTextureAtlas );
        this.overlayLayout.addGlimpseMouseMotionListener( new GlimpseMouseMotionListener( )
        {
            @Override
            public void mouseMoved( GlimpseMouseEvent e )
            {
                tooltipPainter.setLocation( e );
            }
        } );

        this.overlayLayout.addPainter( this.selectedTimePainter );
        this.overlayLayout.addPainter( this.tooltipPainter );

        this.timelineMouseListener = createTimeAxisListener( );
        this.underlayLayout.addGlimpseMouseAllListener( this.timelineMouseListener );
    }

    protected void initializeTimePlot( )
    {
        TaggedAxis1D timeAxis = getTimeAxis( );

        this.addTimeTags( getTimeAxis( ) );

        this.minTag = timeAxis.getTag( MIN_TIME );
        this.maxTag = timeAxis.getTag( MAX_TIME );
        this.currentTag = timeAxis.getTag( CURRENT_TIME );

        PlotInfo info = createPlot( TIMELINE );
        this.timelineInfo = new PlotInfoWrapper( info )
        {
            protected boolean doAnyOtherPlotsGrow( )
            {
                for ( PlotInfo plot : getAllPlots( ) )
                {
                    if ( this != plot && plot.isGrow( ) ) return true;
                }
                
                return false;
            }

            @Override
            public void updateLayout( int index )
            {
                // grow if no other plots are growing
                setGrow( !doAnyOtherPlotsGrow( ) );
                
                super.updateLayout( index );
                
                if ( timeLayout == null ) return;
                
                // push the timeline plot over so that it lines up with the plot labels
                if ( isTimeAxisHorizontal( ) )
                {
                    timeLayout.setLayoutData( String.format( "push, grow, gapleft %d!", labelLayoutSize ) );
                }
                else
                {
                    timeLayout.setLayoutData( String.format( "push, grow, gaptop %d!", labelLayoutSize ) );
                }
            }
        };
        this.stackedPlots.put( this.timelineInfo.getId( ), this.timelineInfo );

        this.timelineInfo.setPlotSpacing( 0 );

        if ( isTimeAxisHorizontal( ) )
        {
            this.timelineInfo.setSize( 45 );
            this.timelineInfo.setOrder( Integer.MAX_VALUE );

            this.timeLayout = new GlimpseAxisLayoutX( this.timelineInfo.getLayout( ) );
            this.timeLayout.setEventConsumer( false );

            this.labelLayoutSize = 30;
        }
        else
        {
            this.timelineInfo.setSize( 60 );
            this.timelineInfo.setOrder( Integer.MIN_VALUE );

            this.timeLayout = new GlimpseAxisLayoutY( this.timelineInfo.getLayout( ) );
            this.timeLayout.setEventConsumer( false );

            this.labelLayoutSize = 30;
        }

        this.timelineInfo.getLayout( ).setEventConsumer( false );

        this.timeAxisPainter = createTimeAxisPainter( );
        this.timeAxisPainter.setFont( getDefaultPlain( 12 ), false );
        this.timeAxisPainter.showCurrentTimeLabel( false );
        this.timeAxisPainter.setCurrentTimeTickColor( GlimpseColor.getGreen( ) );

        this.setBorderSize( 0 );

        this.timeAxisDelegate = new DelegatePainter( );
        this.timeAxisDelegate.addPainter( this.timeAxisPainter );

        this.timeLayout.addPainter( this.timeAxisDelegate );

        this.timeUnitsPainter = new SimpleTextPainter( );
        this.timeUnitsPainter.setHorizontalPosition( HorizontalPosition.Right );
        this.timeUnitsPainter.setVerticalPosition( VerticalPosition.Bottom );
        this.timeUnitsPainter.setColor( GlimpseColor.getBlack( ) );
        this.timeUnitsPainter.setFont( getDefaultBold( 12 ) );
        this.timeUnitsPainter.setText( "GMT" );
        this.timeUnitsPainter.setBackgroundColor( GlimpseColor.getYellow( ) );
        this.timeUnitsPainter.setPaintBackground( true );

        this.timeAxisBorderPainter = new BorderPainter( );
        this.timeAxisBorderPainter.setVisible( false );

        this.timeLayout.addPainter( this.timeUnitsPainter );
        this.timeLayout.addPainter( this.timeAxisBorderPainter );

        this.validate( );
    }

    protected DataAxisMouseListener1D createDataAxisListener( PlotInfo plotInfo )
    {
        return new DataAxisMouseListener1D( this, plotInfo );
    }

    protected TimeAxisMouseListener1D createTimeAxisListener( )
    {
        return new TimeAxisMouseListener1D( this );
    }

    protected TimeAxisPainter createTimeAxisPainter( )
    {
        TimeAxisPainter painter;
        if ( isTimeAxisHorizontal( ) )
        {
            painter = new TimeXAxisPainter( this.epoch );
        }
        else
        {
            painter = new TimeYAxisPainter( this.epoch );
        }

        painter.setFont( getDefaultPlain( 12 ), false );
        painter.showCurrentTimeLabel( false );
        painter.setCurrentTimeTickColor( GlimpseColor.getGreen( ) );

        return painter;
    }

    protected void addTimeTags( TaggedAxis1D axis )
    {
        axis.addTag( MIN_TIME, 0 );
        axis.addTag( MAX_TIME, 10 );
        axis.addTag( CURRENT_TIME, 10 );

        axis.addConstraint( new Constraint( )
        {
            @Override
            public void applyConstraint( TaggedAxis1D axis )
            {
                Tag minTag = axis.getTag( MIN_TIME );
                Tag maxTag = axis.getTag( MAX_TIME );
                Tag currentTag = axis.getTag( CURRENT_TIME );

                double minValue = minTag.getValue( );
                double maxValue = maxTag.getValue( );
                double currentValue = currentTag.getValue( );

                if ( minValue > maxValue )
                {
                    minTag.setValue( maxValue );
                }

                if ( currentValue < minValue )
                {
                    currentTag.setValue( minValue );
                }
                else if ( currentValue > maxValue )
                {
                    currentTag.setValue( maxValue );
                }
            }

            @Override
            public String getName( )
            {
                return "order";
            }
        } );
    }

    @Override
    protected TaggedAxis1D createCommonAxis( )
    {
        return new TaggedAxis1D( );
    }

    protected EventPlotInfo createEventPlot0( PlotInfo plotInfo, TextureAtlas atlas )
    {
        TimePlotInfo timePlot = createTimePlot0( plotInfo );

        // don't show axes
        timePlot.getAxisPainter( ).setVisible( false );
        // don't show grid lines
        timePlot.getGridPainter( ).setVisible( false );
        // center the labels because the plots are so small anyway
        timePlot.getLabelPainter( ).setVerticalPosition( VerticalPosition.Center );

        // the TimeAxisMouseListener1D for all plots is attached to the underlay layout
        // thus we need to let events fall through if they are not handled
        timePlot.getLayout( ).setEventConsumer( false );

        EventPlotInfo eventPlotInfo = new EventPlotInfo( timePlot, atlas );
        eventPlotInfo.setLookAndFeel( laf );

        return eventPlotInfo;
    }

    protected EventPlotInfo createEventPlot0( PlotInfo plotInfo )
    {
        return createEventPlot0( plotInfo, defaultTextureAtlas );
    }

    protected TimePlotInfo createTimePlot0( PlotInfo plotInfo )
    {
        // create a tick handler to calculate Y axis tick marks
        GridAxisLabelHandler labelHandler = new GridAxisLabelHandler( )
        {
            @Override
            protected String tickString( double number, int orderAxis )
            {
                return tickNumberFormatter.format( number );
            }

            @Override
            protected void updateFormatter( int orderAxis, int orderTick )
            {
                tickNumberFormatter.setMaximumFractionDigits( Math.abs( orderTick ) );
            }
        };

        GlimpseAxisLayout2D layout2D = plotInfo.getLayout( );
        layout2D.setEventConsumer( false );

        GlimpseAxisLayout2D plotLayout = new GlimpseAxisLayout2D( layout2D, String.format( "%s-plot", plotInfo.getId( ) ), layout2D.getAxis( ) );
        plotLayout.setEventConsumer( false );

        BackgroundPainter backgroundPainter = new BackgroundPainter( false );
        plotLayout.addPainter( backgroundPainter, Integer.MIN_VALUE );

        // add a painter for user data
        DelegatePainter dataPainter = new DelegatePainter( );
        plotLayout.addPainter( dataPainter );

        AxisLabelHandler xHandler, yHandler;
        if ( orient == HORIZONTAL )
        {
            xHandler = labelHandler;
            yHandler = timeAxisPainter.getLabelHandler( );
        }
        else
        {
            yHandler = labelHandler;
            xHandler = timeAxisPainter.getLabelHandler( );
        }

        // create a painter to display Y axis grid lines
        GridPainter gridPainter = new GridPainter( xHandler, yHandler );
        gridPainter.setShowMinorGrid( false );
        plotLayout.addPainter( gridPainter );

        // create a painter to display Y axis tick marks along the left edge of the graph
        NumericXYAxisPainter axisPainter = new NumericXYAxisPainter( xHandler, yHandler );
        axisPainter.setFont( getDefaultPlain( 9 ), false );
        axisPainter.setShowLabelsNearOrigin( true );
        axisPainter.setShowOriginLabel( true );
        plotLayout.addPainter( axisPainter );

        // add a border
        BorderPainter borderPainter = new BorderPainter( );
        plotLayout.addPainter( borderPainter );

        // create a custom mouse listener for the data (non-time) axis
        DataAxisMouseListener1D listener = createDataAxisListener( plotInfo );
        GlimpseAxisLayout1D layout1D;
        if ( orient == HORIZONTAL )
        {
            layout1D = new GlimpseAxisLayoutX( plotLayout );
        }
        else
        {
            layout1D = new GlimpseAxisLayoutY( plotLayout );
        }
        layout1D.setEventConsumer( false );
        layout1D.addGlimpseMouseAllListener( listener );

        // the TimeAxisMouseListener1D for all plots is attached to the underlay layout
        // thus we need to let events fall through if they are not handled
        plotLayout.setEventConsumer( false );

        // create a GlimpseLayout which will appear to the side of the timeline and contain labels/controls
        GlimpseLayout labelLayout = new GlimpseLayout( layout2D, String.format( "%s-label", plotInfo.getId( ) ) );

        // add a label to display the plot title
        SimpleTextPainter labelPainter = new SimpleTextPainter( );
        labelPainter.setHorizontalPosition( HorizontalPosition.Center );
        labelPainter.setVerticalPosition( VerticalPosition.Center );
        labelPainter.setFont( FontUtils.getDefaultBold( 9 ), false );
        labelPainter.setPadding( 2 );
        // don't use the plot unique identifier as the label by default, this makes
        // it too easy to think that the String argument to createPlot() is supposed to be the label
        labelPainter.setText( "" );
        labelPainter.setHorizontalLabels( false );
        labelLayout.addPainter( labelPainter );

        // add a border
        BorderPainter labelBorderPainter = new BorderPainter( );
        labelBorderPainter.setVisible( false );
        labelLayout.addPainter( labelBorderPainter );

        //@formatter:off
        TimePlotInfo timePlotInfo = new TimePlotInfoImpl( StackedTimePlot2D.this,
                                                      plotInfo,
                                                      plotLayout,
                                                      labelLayout,
                                                      listener,
                                                      gridPainter,
                                                      axisPainter,
                                                      labelPainter,
                                                      borderPainter,
                                                      labelBorderPainter,
                                                      backgroundPainter,
                                                      dataPainter );
        //@formatter:on

        if ( orient == HORIZONTAL )
        {
            gridPainter.setShowHorizontalLines( false );
            labelHandler.setTickSpacing( 45 );
            axisPainter.setShowVerticalTicks( false );
            axisPainter.setShowHorizontalTicks( true );
            axisPainter.setLockTop( true );
        }
        else
        {
            gridPainter.setShowVerticalLines( false );
            labelHandler.setTickSpacing( 16 );
            axisPainter.setShowVerticalTicks( true );
            axisPainter.setShowHorizontalTicks( false );
            axisPainter.setLockLeft( true );
        }

        timePlotInfo.setLookAndFeel( laf );

        return timePlotInfo;
    }
}