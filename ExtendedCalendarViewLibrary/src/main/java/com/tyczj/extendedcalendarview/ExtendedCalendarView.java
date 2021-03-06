package com.tyczj.extendedcalendarview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ExtendedCalendarView extends RelativeLayout implements OnItemClickListener,
	OnClickListener{
	
	private OnDayClickListener dayListener;
    private OnDaySelectListener selectListener;
	private GridView calendar;
	private CalendarAdapter mAdapter;
	private Calendar cal;
	private TextView month;
	private RelativeLayout base;
	private ImageView next,prev;
	private int gestureType = 0;
	private final GestureDetector calendarGesture = new GestureDetector(getContext(), new GestureListener());
	
	public static final int NO_GESTURE = 0;
	public static final int LEFT_RIGHT_GESTURE = 1;
	public static final int UP_DOWN_GESTURE = 2;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private SelectedDate mLastSelectedDate;
	
	public interface OnDayClickListener{
		public void onDayClicked(AdapterView<?> adapter, View view, int position, long id, Day day);
	}

    public static class SelectedDate {
        public int year;
        public int month;
        public int index;   // Note: this is not day of month, but index within the GridView
    }

    public interface OnDaySelectListener {
        public void onDaySelected(Day day);
    }

	public ExtendedCalendarView(Context context) {
        this(context, null);
	}
	
	public ExtendedCalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.extendedCalendarViewStyle);
	}
	
	public ExtendedCalendarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

    private int getColor() {
        return 0;
    }

	private void init(Context context, AttributeSet attrs, int defStyle){

        // load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ExtendedCalendarView, defStyle, 0);

        int backgroundColor = 0;
        ColorStateList dateTextColor = getDefaultColorStateList(0);;
        ColorStateList disabledTextColor = getDefaultColorStateList(0);;
        int weekTextColor = 0;
        int titleTextColor = 0;
        int titleBackgroundColor = 0;
        Drawable singleEventIndicator = getDefaultDrawable(R.drawable.single_event_default);
        Drawable multiEventIndicator = getDefaultDrawable(R.drawable.multi_event_default);

        int n = attributes.getIndexCount();
        for (int i=0; i<n; i++) {
            int attr = attributes.getIndex(i);

            if (attr == R.styleable.ExtendedCalendarView_backgroundColor) {
                backgroundColor = getColor(attributes, R.styleable.ExtendedCalendarView_backgroundColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_dateTextColor) {
                dateTextColor = getColorStateList(attributes, R.styleable.ExtendedCalendarView_dateTextColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_disabledTextColor) {
                disabledTextColor = getColorStateList(attributes, R.styleable.ExtendedCalendarView_disabledTextColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_weekTextColor) {
                weekTextColor = getColor(attributes, R.styleable.ExtendedCalendarView_weekTextColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_titleTextColor) {
                titleTextColor = getColor(attributes, R.styleable.ExtendedCalendarView_titleTextColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_titleBackgroundColor) {
                titleBackgroundColor = getColor(attributes, R.styleable.ExtendedCalendarView_titleBackgroundColor, 0);
            } else if (attr == R.styleable.ExtendedCalendarView_singleEventIcon) {
                singleEventIndicator = getDrawable(attributes, R.styleable.ExtendedCalendarView_singleEventIcon, R.drawable.single_event_default);
            } else if (attr == R.styleable.ExtendedCalendarView_multiEventIcon) {
                multiEventIndicator = getDrawable(attributes, R.styleable.ExtendedCalendarView_multiEventIcon, R.drawable.multi_event_default);
            }
        }

        attributes.recycle();

        cal = Calendar.getInstance();
		base = new RelativeLayout(context);
		base.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		base.setMinimumHeight(50);
        base.setBackgroundColor(titleBackgroundColor);
		
		base.setId(4);
		
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		params.leftMargin = 16;
		params.topMargin = 50;
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		prev = new ImageView(context);
		prev.setId(1);
		prev.setLayoutParams(params);
		prev.setImageResource(R.drawable.navigation_previous_item);
		prev.setOnClickListener(this);
		base.addView(prev);
		
		params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		month = new TextView(context);
		month.setId(2);
		month.setLayoutParams(params);
		month.setTextAppearance(context, android.R.attr.textAppearanceLarge);
		month.setText(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())+" "+cal.get(Calendar.YEAR));
		month.setTextSize(25);
        month.setTextColor(titleTextColor);
		
		base.addView(month);
		
		params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		params.rightMargin = 16;
		params.topMargin = 50;
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		next = new ImageView(context);
		next.setImageResource(R.drawable.navigation_next_item);
		next.setLayoutParams(params);
		next.setId(3);
		next.setOnClickListener(this);
		
		base.addView(next);
		
		addView(base);
		
		params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		//params.bottomMargin = 20;
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		params.addRule(RelativeLayout.BELOW, base.getId());
		
		calendar = new GridView(context);
        calendar.setSelector(new ColorDrawable(Color.TRANSPARENT));
		calendar.setLayoutParams(params);
		calendar.setVerticalSpacing(4);
		calendar.setHorizontalSpacing(4);
		calendar.setNumColumns(7);
		calendar.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
		calendar.setDrawSelectorOnTop(true);
        calendar.setBackgroundColor(backgroundColor);

		mAdapter = new CalendarAdapter(context, cal, dateTextColor, disabledTextColor, weekTextColor, singleEventIndicator, multiEventIndicator);
		calendar.setAdapter(mAdapter);
		calendar.setOnTouchListener(new OnTouchListener() {
			
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            return calendarGesture.onTouchEvent(event);
	        }
	    });

        checkLastSelectedDate();
        selectDay(true);
		
		addView(calendar);
	}

    private int getColor(TypedArray attrs, int index, int defValue) {

        // If typed value is a color int,
        // attrs.getValue() will populate
        // typedValue with the color value.
        // If typed value is a reference to
        // an attribute, resolve that attribute
        // to another TypedValue. The value
        // of that TypedValue will now contain
        // the proper color value.

        TypedValue typedValue = new TypedValue();
        boolean retrieved = attrs.getValue(index, typedValue);
        if (retrieved) {
            if (typedValue.type == TypedValue.TYPE_ATTRIBUTE) {
                Resources.Theme theme = getContext().getTheme();
                theme.resolveAttribute(typedValue.data, typedValue, true);
            }
            return typedValue.data;
        } else {
            return defValue;
        }
    }

    private ColorStateList getColorStateList(TypedArray attrs, int index, int defValue){

        // If typed value is a ColorStateList,
        // attrs.getValue() will populate
        // typedValue with the proper reference
        // to a ColorStateList. If typed value
        // is a reference to an attribute,
        // resolve that attribute to another
        // TypedValue. The value of that
        // TypedValue will now contain
        // the reference to a ColorStateList.

        TypedValue typedValue = new TypedValue();
        boolean retrieved = attrs.getValue(index, typedValue);
        if (retrieved) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                // typedValue is pure color
                return getDefaultColorStateList(typedValue.data);
            } else {
                if (typedValue.type == TypedValue.TYPE_ATTRIBUTE) {
                    // typedValue is attribute
                    // we must resolve it to
                    // obtain the proper resource id
                    Resources.Theme theme = getContext().getTheme();
                    theme.resolveAttribute(typedValue.data, typedValue, true);
                }
                return getContext().getResources().getColorStateList(typedValue.resourceId);
            }
        } else {
            return getDefaultColorStateList(defValue);
        }
    }

    private Drawable getDrawable(TypedArray attrs, int index, int defValue) {

        // If typed value is a drawable,
        // attrs.getValue() will populate
        // typedValue with the proper reference
        // to a drawable. If typed value
        // is a reference to an attribute,
        // resolve that attribute to another
        // TypedValue. The value of that
        // TypedValue will now contain
        // the reference to a drawable.

        TypedValue typedValue = new TypedValue();
        boolean retrieved = attrs.getValue(index, typedValue);
        if (retrieved) {
            if (typedValue.type == TypedValue.TYPE_ATTRIBUTE) {
                // typedValue is attribute
                // we must resolve it to
                // obtain the proper resource id
                Resources.Theme theme = getContext().getTheme();
                theme.resolveAttribute(typedValue.data, typedValue, true);
            }
            return getContext().getResources().getDrawable(typedValue.resourceId);
        } else {
            return getDefaultDrawable(defValue);
        }
    }

    private ColorStateList getDefaultColorStateList(int defValue) {
        return new ColorStateList(
                new int[][]{
                        new int[] {}
                },
                new int[] {
                        defValue
                }
        );
    }

    private Drawable getDefaultDrawable(int defValue) {
        return getContext().getResources().getDrawable(defValue);
    }

	private class GestureListener extends SimpleOnGestureListener {
	    @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
	    	
	    	if(gestureType == LEFT_RIGHT_GESTURE){
	    		if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		            nextMonth();
		            return true; // Right to left
		        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
		            previousMonth();
		            return true; // Left to right
		        }
	    	}else if(gestureType == UP_DOWN_GESTURE){
	        	if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
		        	nextMonth();
		            return true; // Bottom to top
		        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
		        	previousMonth();
		            return true; // Top to bottom
		        }
	        }
	        return false;
	    }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Day d = (Day) mAdapter.getItem(position);
        checkLastSelectedDate();

        if (d.getYear() == cal.get(Calendar.YEAR) && d.getMonth() == cal.get(Calendar.MONTH)) {
            if (d.getDay() != 0) {

                mLastSelectedDate.year = cal.get(Calendar.YEAR);
                mLastSelectedDate.month = cal.get(Calendar.MONTH);
                mLastSelectedDate.index = position;
                selectDay(false);

                if (dayListener != null) {
                    dayListener.onDayClicked(parent, view, position, id, d);
                }
            } else {
                selectDay(true);
            }
        } else {
            calendar.setItemChecked(position, false);
            selectDay(true);
            if (d.getYear() < cal.get(Calendar.YEAR) || d.getMonth() < cal.get(Calendar.MONTH)) {
                previousMonth();
            } else {
                nextMonth();
            }
        }
    }

	/**
	 * 
	 * @param listener
	 * 
	 * Set a listener for when you press on a day in the month
	 */
	public void setOnDayClickListener(OnDayClickListener listener){
		if(calendar != null){
			dayListener = listener;
			calendar.setOnItemClickListener(this);
		}
	}

    public void setOnDaySelectListener(OnDaySelectListener listener) {
        if (calendar != null) {
            selectListener = listener;
            calendar.setOnItemClickListener(this);

            // Resend last selected date
            checkLastSelectedDate();
            selectListener.onDaySelected((Day) mAdapter.getItem(mLastSelectedDate.index));
        }
    }

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case 1:
			previousMonth();
			break;
		case 3:
			nextMonth();
			break;
		default:
			break;
		}
	}
	
	public void previousMonth(){
		if(cal.get(Calendar.MONTH) == cal.getActualMinimum(Calendar.MONTH)) {				
			cal.set((cal.get(Calendar.YEAR)-1),cal.getActualMaximum(Calendar.MONTH),1);
		} else {
			cal.set(Calendar.MONTH,cal.get(Calendar.MONTH)-1);
		}
		rebuildCalendar();
	}
	
	public void nextMonth(){
		if(cal.get(Calendar.MONTH) == cal.getActualMaximum(Calendar.MONTH)) {				
			cal.set((cal.get(Calendar.YEAR)+1),cal.getActualMinimum(Calendar.MONTH),1);
		} else {
			cal.set(Calendar.MONTH,cal.get(Calendar.MONTH)+1);
		}
		rebuildCalendar();
	}
	
	private void rebuildCalendar(){
		if(month != null){
			month.setText(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())+" "+cal.get(Calendar.YEAR));
			refreshCalendar();
		}
	}
	
	/**
	 * Refreshes the month
	 */
	public void refreshCalendar(){
		mAdapter.refreshDays();
		mAdapter.notifyDataSetChanged();

        checkLastSelectedDate();
        selectDay(true);
	}

    private void checkLastSelectedDate() {
        if(mLastSelectedDate == null) {
            mLastSelectedDate = new SelectedDate();
            Calendar today = GregorianCalendar.getInstance();
            mLastSelectedDate.year = today.get(Calendar.YEAR);
            mLastSelectedDate.month = today.get(Calendar.MONTH);
            int todayDay = today.get(Calendar.DAY_OF_MONTH);
            today.set(Calendar.DAY_OF_MONTH, 1);
            int offset = today.get(Calendar.DAY_OF_WEEK);

            mLastSelectedDate.index = todayDay + offset + 5;
        }
    }

    private void selectDay(boolean forceCheck) {
        if (mLastSelectedDate != null) {
            if (cal.get(Calendar.YEAR) == mLastSelectedDate.year && cal.get(Calendar.MONTH) == mLastSelectedDate.month) {
                if (forceCheck) {
                    calendar.setItemChecked(mLastSelectedDate.index, true);
                }
                if (selectListener != null) {
                    selectListener.onDaySelected((Day) mAdapter.getItem(mLastSelectedDate.index));
                }
            } else {
                calendar.setItemChecked(mLastSelectedDate.index, false);
            }
        }
    }

	/**
	 * 
	 * @param color
	 * 
	 * Sets the background color of the month bar
	 */
	public void setMonthTextBackgroundColor(int color){
		base.setBackgroundColor(color);
	}
	
	@SuppressLint("NewApi")
	/**
	 * 
	 * @param drawable
	 * 
	 * Sets the background color of the month bar. Requires at least API level 16
	 */
	public void setMonthTextBackgroundDrawable(Drawable drawable){
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
			base.setBackground(drawable);
		}
		
	}
	
	/**
	 * 
	 * @param resource
	 * 
	 * Sets the background color of the month bar
	 */
	public void setMonehtTextBackgroundResource(int resource){
		base.setBackgroundResource(resource);
	}
	
	/**
	 * 
	 * @param recource
	 * 
	 * change the image of the previous month button
	 */
	public void setPreviousMonthButtonImageResource(int recource){
		prev.setImageResource(recource);
	}
	
	/**
	 * 
	 * @param bitmap
	 * 
	 * change the image of the previous month button
	 */
	public void setPreviousMonthButtonImageBitmap(Bitmap bitmap){
		prev.setImageBitmap(bitmap);
	}
	
	/**
	 * 
	 * @param drawable
	 * 
	 * change the image of the previous month button
	 */
	public void setPreviousMonthButtonImageDrawable(Drawable drawable){
		prev.setImageDrawable(drawable);
	}
	
	/**
	 * 
	 * @param recource
	 * 
	 * change the image of the next month button
	 */
	public void setNextMonthButtonImageResource(int recource){
		next.setImageResource(recource);
	}
	
	/**
	 * 
	 * @param bitmap
	 * 
	 * change the image of the next month button
	 */
	public void setNextMonthButtonImageBitmap(Bitmap bitmap){
		next.setImageBitmap(bitmap);
	}
	
	/**
	 * 
	 * @param drawable
	 * 
	 * change the image of the next month button
	 */
	public void setNextMonthButtonImageDrawable(Drawable drawable){
		next.setImageDrawable(drawable);
	}
	
	/**
	 * 
	 * @param gestureType
	 * 
	 * Allow swiping the calendar left/right or up/down to change the month. 
	 * 
	 * Default value no gesture
	 */
	public void setGesture(int gestureType){
		this.gestureType = gestureType;
	}

}
