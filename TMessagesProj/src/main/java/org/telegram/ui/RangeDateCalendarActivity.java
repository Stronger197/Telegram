package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public class RangeDateCalendarActivity extends BaseFragment {

    FrameLayout contentView;

    RecyclerListView listView;
    LinearLayoutManager layoutManager;
    TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextPaint activeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextPaint textPaint2 = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextView clearHistoryTextView;

    Paint rangeSelectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long dialogId;
    private boolean loading;
    private boolean checkEnterItems;


    int startFromYear;
    int startFromMonth;
    int monthCount;
    int daysCount = 0;

    CalendarAdapter adapter;
    Callback callback;


    SparseArray<SparseArray<PeriodDay>> messagesByYearMounth = new SparseArray<>();
    boolean endReached;
    int startOffset = 0;
    int lastId;
    int minMontYear;
    private int photosVideosTypeFilter;
    private boolean isOpened;
    int selectedYear;
    int selectedMonth;

    boolean isSelectState = false;

    Path clipOutPath = new Path();


    enum CellState {
        EMPTY, START, IN_RANGE, END
    }


    public RangeDateCalendarActivity(Bundle args, int photosVideosTypeFilter, int selectedDate) {
        super(args);
        this.photosVideosTypeFilter = photosVideosTypeFilter;

        if (selectedDate != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDate * 1000L);
            selectedYear = calendar.get(Calendar.YEAR);
            selectedMonth = calendar.get(Calendar.MONTH);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        dialogId = getArguments().getLong("dialog_id");
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        rangeSelectorPaint.setColor(0x50a5e6);
        textPaint.setTextSize(AndroidUtilities.dp(16));
        textPaint.setTextAlign(Paint.Align.CENTER);

        textPaint2.setTextSize(AndroidUtilities.dp(11));
        textPaint2.setTextAlign(Paint.Align.CENTER);
        textPaint2.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        activeTextPaint.setTextSize(AndroidUtilities.dp(16));
        activeTextPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        activeTextPaint.setTextAlign(Paint.Align.CENTER);

        contentView = new FrameLayout(context);
        createActionBar(context);
        contentView.addView(actionBar);
        actionBar.setTitle(LocaleController.getString("Calendar", R.string.Calendar));
        actionBar.setCastShadows(false);

        listView = new RecyclerListView(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                checkEnterItems = false;
            }
        };
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        layoutManager.setReverseLayout(true);
        listView.setAdapter(adapter = new CalendarAdapter());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkLoadNext();
            }
        });

        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0, 0, 36, 0, AndroidUtilities.dp(20)));

        FrameLayout buttonCell = new FrameLayout(context);
        buttonCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        contentView.addView(buttonCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(20), Gravity.BOTTOM, 0, 0, 0, 0));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        contentView.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 0, 0, 0, AndroidUtilities.dp(20)));

        clearHistoryTextView = new TextView(context);
        clearHistoryTextView.setText(LocaleController.getString("CalendarSelectDays", R.string.CalendarSelectDays));
        clearHistoryTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue));
        buttonCell.addView(clearHistoryTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        buttonCell.setOnClickListener(v -> {
            if(!isSelectState) {
                isSelectState = true;
                clearHistoryTextView.setTextColor(Theme.getColor(Theme.key_dialogTextRed));
                clearHistoryTextView.setText(LocaleController.getString("CalendarClearHistory", R.string.CalendarClearHistory));
                clearHistoryTextView.setAlpha(0.5f);
                return;
            }

            if(firstSelectedDay != null) {
                Calendar firstDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                firstDayCalendar.set(firstSelectedDay.year, firstSelectedDay.month, firstSelectedDay.day, 0, 0, 0);

                Calendar secondDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                if(secondSelectedDay != null) {
                    secondDayCalendar.set(secondSelectedDay.year, secondSelectedDay.month, secondSelectedDay.day, 23, 59, 59);
                } else {
                    secondDayCalendar.set(firstSelectedDay.year, firstSelectedDay.month, firstSelectedDay.day, 23, 59, 59);
                }
                secondDayCalendar.setTimeZone(TimeZone.getDefault());

                long firstTimeZoneOffset = TimeZone.getDefault().getOffset(firstDayCalendar.getTimeInMillis());
                long secondTimeZoneOffset = TimeZone.getDefault().getOffset(secondDayCalendar.getTimeInMillis());

                callback.onDateSelected((int) ((firstDayCalendar.getTimeInMillis() - firstTimeZoneOffset) / 1000), (int) ((secondDayCalendar.getTimeInMillis() - secondTimeZoneOffset) / 1000), daysCount);
            }
        });

        final String[] daysOfWeek = new String[]{
            LocaleController.getString("CalendarWeekNameShortMonday", R.string.CalendarWeekNameShortMonday),
            LocaleController.getString("CalendarWeekNameShortTuesday", R.string.CalendarWeekNameShortTuesday),
            LocaleController.getString("CalendarWeekNameShortWednesday", R.string.CalendarWeekNameShortWednesday),
            LocaleController.getString("CalendarWeekNameShortThursday", R.string.CalendarWeekNameShortThursday),
            LocaleController.getString("CalendarWeekNameShortFriday", R.string.CalendarWeekNameShortFriday),
            LocaleController.getString("CalendarWeekNameShortSaturday", R.string.CalendarWeekNameShortSaturday),
            LocaleController.getString("CalendarWeekNameShortSunday", R.string.CalendarWeekNameShortSunday),
        };

        Drawable headerShadowDrawable = ContextCompat.getDrawable(context, R.drawable.header_shadow).mutate();

        View calendarSignatureView = new View(context) {

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float xStep = getMeasuredWidth() / 7f;
                for (int i = 0; i < 7; i++) {
                    float cx = xStep * i + xStep / 2f;
                    float cy = (getMeasuredHeight() - AndroidUtilities.dp(2)) / 2f;
                    canvas.drawText(daysOfWeek[i], cx, cy + AndroidUtilities.dp(5), textPaint2);
                }
                headerShadowDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(3), getMeasuredWidth(), getMeasuredHeight());
                headerShadowDrawable.draw(canvas);
            }
        };

        contentView.addView(calendarSignatureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, 0, 0, 0, 0, 0));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = contentView;

        Calendar calendar = Calendar.getInstance();
        startFromYear = calendar.get(Calendar.YEAR);
        startFromMonth = calendar.get(Calendar.MONTH);

        if (selectedYear != 0) {
            monthCount = (startFromYear - selectedYear) * 12 + startFromMonth - selectedMonth + 1;
            layoutManager.scrollToPositionWithOffset(monthCount - 1, AndroidUtilities.dp(120));
        }
        if (monthCount < 3) {
            monthCount = 3;
        }


        loadNext();
        updateColors();
        activeTextPaint.setColor(Color.WHITE);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        return fragmentView;
    }

    private void updateColors() {
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        activeTextPaint.setColor(Color.WHITE);
        textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textPaint2.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_listSelector), false);
    }

    private void loadNext() {
        if (loading || endReached) {
            return;
        }
        loading = true;
        TLRPC.TL_messages_getSearchResultsCalendar req = new TLRPC.TL_messages_getSearchResultsCalendar();
        if (photosVideosTypeFilter == SharedMediaLayout.FILTER_PHOTOS_ONLY) {
            req.filter = new TLRPC.TL_inputMessagesFilterPhotos();
        } else if (photosVideosTypeFilter == SharedMediaLayout.FILTER_VIDEOS_ONLY) {
            req.filter = new TLRPC.TL_inputMessagesFilterVideo();
        } else {
            req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
        }

        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.offset_id = lastId;

        Calendar calendar = Calendar.getInstance();
        listView.setItemAnimator(null);
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_messages_searchResultsCalendar res = (TLRPC.TL_messages_searchResultsCalendar) response;

                for (int i = 0; i < res.periods.size(); i++) {
                    TLRPC.TL_searchResultsCalendarPeriod period = res.periods.get(i);
                    calendar.setTimeInMillis(period.date * 1000L);
                    int month = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH);
                    SparseArray<PeriodDay> messagesByDays = messagesByYearMounth.get(month);
                    if (messagesByDays == null) {
                        messagesByDays = new SparseArray<>();
                        messagesByYearMounth.put(month, messagesByDays);
                    }
                    PeriodDay periodDay = new PeriodDay();
                    MessageObject messageObject = new MessageObject(currentAccount, res.messages.get(i), false, false);
                    periodDay.messageObject = messageObject;
                    startOffset += res.periods.get(i).count;
                    periodDay.startOffset = startOffset;
                    int index = calendar.get(Calendar.DAY_OF_MONTH) - 1;
                    if (messagesByDays.get(index, null) == null) {
                        messagesByDays.put(index, periodDay);
                    }
                    if (month < minMontYear || minMontYear == 0) {
                        minMontYear = month;
                    }

                }

                loading = false;
                if (!res.messages.isEmpty()) {
                    lastId = res.messages.get(res.messages.size() - 1).id;
                    endReached = false;
                    checkLoadNext();
                } else {
                    endReached = true;
                }
                if (isOpened) {
                    checkEnterItems = true;
                }
                listView.invalidate();
                int newMonthCount = (int) (((calendar.getTimeInMillis() / 1000) - res.min_date) / 2629800) + 1;
                adapter.notifyItemRangeChanged(0, monthCount);
                if (newMonthCount > monthCount) {
                    adapter.notifyItemRangeInserted(monthCount + 1, newMonthCount);
                    monthCount = newMonthCount;
                }
                if (endReached) {
                    resumeDelayedFragmentAnimation();
                }
            }
        }));
    }

    private void checkLoadNext() {
        if (loading || endReached) {
            return;
        }
        int listMinMonth = Integer.MAX_VALUE;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof MonthView) {
                int currentMonth = ((MonthView) child).currentYear * 100 + ((MonthView) child).currentMonthInYear;
                if (currentMonth < listMinMonth) {
                    listMinMonth = currentMonth;
                }
            }
        }
        ;
        int min1 = (minMontYear / 100 * 12) + minMontYear % 100;
        int min2 = (listMinMonth / 100 * 12) + listMinMonth % 100;
        if (min1 + 3 >= min2) {
            loadNext();
        }
    }
    DateCell firstSelectedDay = null;
    DateCell secondSelectedDay = null;
    private class CalendarAdapter extends RecyclerView.Adapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new MonthView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MonthView monthView = (MonthView) holder.itemView;

            int year = startFromYear - position / 12;
            int month = startFromMonth - position % 12;
            if (month < 0) {
                month += 12;
                year--;
            }
            boolean animated = monthView.currentYear == year && monthView.currentMonthInYear == month;
            monthView.setDate(year, month, messagesByYearMounth.get(year * 100 + month), animated);
        }

        @Override
        public long getItemId(int position) {
            int year = startFromYear - position / 12;
            int month = startFromMonth - position % 12;
            return year * 100L + month;
        }

        @Override
        public int getItemCount() {
            return monthCount;
        }
    }

    private class MonthView extends FrameLayout {

        SimpleTextView titleView;
        int currentYear;
        int currentMonthInYear;
        int daysInMonth;
        int startDayOfWeek;
        int cellCount;
        int startMonthTime;

        SparseArray<PeriodDay> messagesByDays = new SparseArray<>();
        SparseArray<ImageReceiver> imagesByDays = new SparseArray<>();
        ArrayList<DateCell> dateCells = new ArrayList<>();
        SparseArray<PeriodDay> dates = new SparseArray<>();
        SparseArray<PeriodDay> animatedFromMessagesByDays = new SparseArray<>();
        SparseArray<ImageReceiver> animatedFromImagesByDays = new SparseArray<>();

        boolean attached;
        float animationProgress = 1f;

        public MonthView(Context context) {
            super(context);
            setWillNotDraw(false);
            titleView = new SimpleTextView(context);
            titleView.setTextSize(15);
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 28, 0, 0, 12, 0, 4));
        }

        public void setDate(int year, int monthInYear, SparseArray<PeriodDay> messagesByDays, boolean animated) {
            boolean dateChanged = year != currentYear && monthInYear != currentMonthInYear;
            currentYear = year;
            currentMonthInYear = monthInYear;
            this.messagesByDays = messagesByDays;

            if (dateChanged) {
                if (imagesByDays != null) {
                    for (int i = 0; i < imagesByDays.size(); i++) {
                        imagesByDays.valueAt(i).onDetachedFromWindow();
                        imagesByDays.valueAt(i).setParentView(null);
                    }
                    imagesByDays = null;
                }
            }
            if (messagesByDays != null) {
                if (imagesByDays == null) {
                    imagesByDays = new SparseArray<>();
                }

                for (int i = 0; i < messagesByDays.size(); i++) {
                    int key = messagesByDays.keyAt(i);
                    if (imagesByDays.get(key, null) != null) {
                        continue;
                    }
                    ImageReceiver receiver = new ImageReceiver();
                    receiver.setParentView(this);
                    PeriodDay periodDay = messagesByDays.get(key);
                    MessageObject messageObject = periodDay.messageObject;
                    if (messageObject != null) {
                        if (messageObject.isVideo()) {
                            TLRPC.Document document = messageObject.getDocument();
                            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 50);
                            TLRPC.PhotoSize qualityThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
                            if (thumb == qualityThumb) {
                                qualityThumb = null;
                            }
                            if (thumb != null) {
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(ImageLocation.getForDocument(qualityThumb, document), "44_44", messageObject.strippedThumb, null, messageObject, 0);
                                } else {
                                    receiver.setImage(ImageLocation.getForDocument(qualityThumb, document), "44_44", ImageLocation.getForDocument(thumb, document), "b", (String) null, messageObject, 0);
                                }
                            }
                        } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && messageObject.messageOwner.media.photo != null && !messageObject.photoThumbs.isEmpty()) {
                            TLRPC.PhotoSize currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 50);
                            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 320, false, currentPhotoObjectThumb, false);
                            if (messageObject.mediaExists || DownloadController.getInstance(currentAccount).canDownloadMedia(messageObject)) {
                                if (currentPhotoObject == currentPhotoObjectThumb) {
                                    currentPhotoObjectThumb = null;
                                }
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "44_44", null, null, messageObject.strippedThumb, currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                                } else {
                                    receiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "44_44", ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                                }
                            } else {
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(null, null, messageObject.strippedThumb, null, messageObject, 0);
                                } else {
                                    receiver.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", (String) null, messageObject, 0);
                                }
                            }
                        }
                        receiver.setRoundRadius(AndroidUtilities.dp(22));
                        imagesByDays.put(key, receiver);
                    }
                }
            }

            YearMonth yearMonthObject = YearMonth.of(year, monthInYear + 1);
            daysInMonth = yearMonthObject.lengthOfMonth();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, monthInYear, 0);
            startDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 6) % 7;
            startMonthTime = (int) (calendar.getTimeInMillis() / 1000L);

            int totalColumns = daysInMonth + startDayOfWeek;
            cellCount = (int) (totalColumns / 7f) + (totalColumns % 7 == 0 ? 0 : 1);
            calendar.set(year, monthInYear + 1, 0);
            titleView.setText(LocaleController.formatYearMont(calendar.getTimeInMillis() / 1000, true));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(cellCount * (44 + 8) + 44), MeasureSpec.EXACTLY));
        }

        boolean pressed;
        float pressedX;
        float pressedY;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressed = true;
                pressedX = event.getX();
                pressedY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (pressed) {

                    for(int i = 0; i < dateCells.size(); i++) {
                        if (dateCells.get(i).drawRegion.contains(pressedX, pressedY)) {
                            if (callback != null && isSelectState) {
                                daysCount = 0;
                                if(secondSelectedDay != null) {
                                    firstSelectedDay = null;
                                    secondSelectedDay = null;
                                }
                                if(firstSelectedDay == null) {
                                    firstSelectedDay = dateCells.get(i);
                                } else if(dateCells.get(i).equals(firstSelectedDay)) {
                                    firstSelectedDay = dateCells.get(i);
                                } else {
                                    DateCell valueToSet = dateCells.get(i);
                                    if(valueToSet.compare(firstSelectedDay) < 0) {
                                        secondSelectedDay = firstSelectedDay;
                                        firstSelectedDay = valueToSet;
                                    } else {
                                        secondSelectedDay = valueToSet;
                                    }
                                }

                                if(firstSelectedDay == null) {
                                    clearHistoryTextView.setAlpha(0.5f);
                                } else {
                                    clearHistoryTextView.setAlpha(1f);
                                }
                                adapter.notifyItemRangeChanged(0, monthCount);
                            }

                            break;
                        }
                    }
                }
                pressed = false;
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed = false;
            }
            return pressed;
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            dateCells.clear();

            int currentCell = 0;
            int currentColumn = startDayOfWeek;

            float xStep = getMeasuredWidth() / 7f;
            float yStep = AndroidUtilities.dp(44 + 8);
            for (int i = 0; i < daysInMonth; i++) {
                float cx = xStep * currentColumn + xStep / 2f;
                float cy = yStep * currentCell + yStep / 2f + AndroidUtilities.dp(44);

                CellState currentCellState = CellState.EMPTY;
                if(firstSelectedDay != null) {
                    if(firstSelectedDay.day == i + 1 && firstSelectedDay.month == currentMonthInYear && firstSelectedDay.year == currentYear) {
                        currentCellState = CellState.START;
                    }
                }
                if(firstSelectedDay != null && secondSelectedDay != null) {
                    DateCell dateCellToCompare = new DateCell();
                    dateCellToCompare.day = i + 1;
                    dateCellToCompare.month = currentMonthInYear;
                    dateCellToCompare.year = currentYear;

                    if(firstSelectedDay.compare(dateCellToCompare) < 0 && secondSelectedDay.compare(dateCellToCompare) > 0) {
                        currentCellState = CellState.IN_RANGE;
                    }
                }
                if(secondSelectedDay != null) {
                    if(secondSelectedDay.day == i + 1 && secondSelectedDay.month == currentMonthInYear && secondSelectedDay.year == currentYear) {
                        currentCellState = CellState.END;
                    }
                }

                int nowTime = (int) (System.currentTimeMillis() / 1000L);
                if (nowTime < startMonthTime + (i + 1) * 86400) {
                    int oldAlpha = textPaint.getAlpha();
                    textPaint.setAlpha((int) (oldAlpha * 0.3f));
                    canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
                    textPaint.setAlpha(oldAlpha);
                } else if (messagesByDays != null && messagesByDays.get(i, null) != null) {
                    float alpha = 1f;
                    if (imagesByDays.get(i) != null) {
                        if (checkEnterItems && !messagesByDays.get(i).wasDrawn) {
                            messagesByDays.get(i).enterAlpha = 0f;
                            messagesByDays.get(i).startEnterDelay = (cy + getY()) / listView.getMeasuredHeight() * 150;
                        }
                        if (messagesByDays.get(i).startEnterDelay > 0) {
                            messagesByDays.get(i).startEnterDelay -= 16;
                            if (messagesByDays.get(i).startEnterDelay < 0) {
                                messagesByDays.get(i).startEnterDelay = 0;
                            } else {
                                invalidate();
                            }
                        }
                        if (messagesByDays.get(i).startEnterDelay == 0 && messagesByDays.get(i).enterAlpha != 1f) {
                            messagesByDays.get(i).enterAlpha += 16 / 220f;
                            if (messagesByDays.get(i).enterAlpha > 1f) {
                                messagesByDays.get(i).enterAlpha = 1f;
                            } else {
                                invalidate();
                            }
                        }
                        alpha = messagesByDays.get(i).enterAlpha;
                        if (alpha != 1f) {
                            canvas.save();
                            float s = 0.8f + 0.2f * alpha;
                            canvas.scale(s, s, cx, cy);
                        }
                        imagesByDays.get(i).setAlpha(messagesByDays.get(i).enterAlpha);

                        if(currentCellState == CellState.IN_RANGE) {
                            rangeSelectorPaint.setAlpha(128);
                            rangeSelectorPaint.setStyle(Paint.Style.FILL);

                            if (currentColumn + 1 >= 7 || i == daysInMonth - 1) {
                                canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                                clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(44) / 2, Path.Direction.CW);
                                canvas.save();
                                canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                                canvas.drawRect(
                                    cx - xStep / 2,
                                    cy - AndroidUtilities.dp(22),
                                    cx,
                                    cy + AndroidUtilities.dp(22),
                                    rangeSelectorPaint);
                                canvas.restore();
                            } else if(currentColumn == 0 || i == 0) {
                                canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                                clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(44) / 2, Path.Direction.CW);
                                canvas.save();
                                canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                                canvas.drawRect(
                                    cx,
                                    cy - AndroidUtilities.dp(22),
                                    cx + xStep / 2,
                                    cy + AndroidUtilities.dp(22),
                                    rangeSelectorPaint);
                                canvas.restore();
                            } else {
                                canvas.drawRect(
                                    cx - xStep / 2,
                                    cy - AndroidUtilities.dp(22),
                                    cx + xStep / 2,
                                    cy + AndroidUtilities.dp(22),
                                    rangeSelectorPaint);
                            }

                            daysCount++;

                            imagesByDays.get(i).setImageCoords(cx - AndroidUtilities.dp(36) / 2f, cy - AndroidUtilities.dp(36) / 2f, AndroidUtilities.dp(36), AndroidUtilities.dp(36));
                        } else if(currentCellState == CellState.START) {
                            daysCount++;
                            rangeSelectorPaint.setAlpha(255);
                            rangeSelectorPaint.setStyle(Paint.Style.STROKE);

                            rangeSelectorPaint.setStrokeWidth(AndroidUtilities.dp(2));
                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                            rangeSelectorPaint.setAlpha(128);
                            rangeSelectorPaint.setStyle(Paint.Style.FILL);


                            if(secondSelectedDay != null && currentColumn + 1 < 7 && i != daysInMonth - 1) {
                                clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(42) / 2, Path.Direction.CW);

                                canvas.save();
                                canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                                canvas.drawRect(
                                    cx,
                                    cy - AndroidUtilities.dp(22),
                                    cx + xStep / 2,
                                    cy + AndroidUtilities.dp(22),
                                    rangeSelectorPaint);
                                canvas.restore();
                            }

                            imagesByDays.get(i).setImageCoords(cx - AndroidUtilities.dp(36) / 2f, cy - AndroidUtilities.dp(36) / 2f, AndroidUtilities.dp(36), AndroidUtilities.dp(36));
                        } else if(currentCellState == CellState.END) {
                            daysCount++;
                            rangeSelectorPaint.setAlpha(255);
                            rangeSelectorPaint.setStyle(Paint.Style.STROKE);

                            rangeSelectorPaint.setStrokeWidth(AndroidUtilities.dp(2));
                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                            rangeSelectorPaint.setAlpha(128);
                            rangeSelectorPaint.setStyle(Paint.Style.FILL);


                            if(!firstSelectedDay.equals(secondSelectedDay) && currentColumn != 0 && i != 0) {
                                clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(42) / 2, Path.Direction.CW);

                                canvas.save();
                                canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                                canvas.drawRect(
                                    cx - xStep / 2,
                                    cy - AndroidUtilities.dp(22),
                                    cx,
                                    cy + AndroidUtilities.dp(22),
                                    rangeSelectorPaint);
                                canvas.restore();
                            }

                            imagesByDays.get(i).setImageCoords(cx - AndroidUtilities.dp(36) / 2f, cy - AndroidUtilities.dp(36) / 2f, AndroidUtilities.dp(36), AndroidUtilities.dp(36));
                        } else {
                            imagesByDays.get(i).setImageCoords(cx - AndroidUtilities.dp(44) / 2f, cy - AndroidUtilities.dp(44) / 2f, AndroidUtilities.dp(44), AndroidUtilities.dp(44));
                        }

                        imagesByDays.get(i).draw(canvas);
                        messagesByDays.get(i).wasDrawn = true;
                        if (alpha != 1f) {
                            canvas.restore();
                        }
                    }
                    if (alpha != 1f) {
                        int oldAlpha = textPaint.getAlpha();
                        textPaint.setAlpha((int) (oldAlpha * (1f - alpha)));
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
                        textPaint.setAlpha(oldAlpha);

                        oldAlpha = textPaint.getAlpha();
                        activeTextPaint.setAlpha((int) (oldAlpha * alpha));
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), activeTextPaint);
                        activeTextPaint.setAlpha(oldAlpha);
                    } else {
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), activeTextPaint);
                    }

                    DateCell dateCell = new DateCell();
                    dateCell.day = i + 1;
                    dateCell.month = currentMonthInYear;
                    dateCell.year = currentYear;
                    dateCell.drawRegion.set(cx - xStep / 2, cy - AndroidUtilities.dp(40), cx + xStep / 2, cy + AndroidUtilities.dp(40));
                    dateCells.add(dateCell);
                } else {
                    if(currentCellState == CellState.IN_RANGE) {
                        rangeSelectorPaint.setAlpha(128);
                        rangeSelectorPaint.setStyle(Paint.Style.FILL);
                        if (currentColumn + 1 >= 7 || i == daysInMonth - 1) {


                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);


                            clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(44) / 2, Path.Direction.CW);
                            canvas.save();
                            canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                            canvas.drawRect(
                                cx - xStep / 2,
                                cy - AndroidUtilities.dp(22),
                                cx,
                                cy + AndroidUtilities.dp(22),
                                rangeSelectorPaint);
                            canvas.restore();
                        } else if(currentColumn == 0 || i == 0) {
                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);


                            clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(44) / 2, Path.Direction.CW);
                            canvas.save();
                            canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                            canvas.drawRect(
                                cx,
                                cy - AndroidUtilities.dp(22),
                                cx + xStep / 2,
                                cy + AndroidUtilities.dp(22),
                                rangeSelectorPaint);
                            canvas.restore();
                        } else {
                            canvas.drawRect(
                                cx - xStep / 2,
                                cy - AndroidUtilities.dp(22),
                                cx + xStep / 2,
                                cy + AndroidUtilities.dp(22),
                                rangeSelectorPaint);
                        }
                        daysCount++;
                    }
                    if(currentCellState == CellState.START) {
                        textPaint.setColor(Color.WHITE);
                        rangeSelectorPaint.setAlpha(255);
                        rangeSelectorPaint.setStyle(Paint.Style.FILL);

                        daysCount++;

                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(36) / 2, rangeSelectorPaint);


                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(36) / 2, rangeSelectorPaint);

                        rangeSelectorPaint.setStyle(Paint.Style.STROKE);
                        rangeSelectorPaint.setStrokeWidth(AndroidUtilities.dp(2));
                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                        clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(42) / 2, Path.Direction.CW);

                        rangeSelectorPaint.setAlpha(128);
                        rangeSelectorPaint.setStyle(Paint.Style.FILL);

                        if(secondSelectedDay != null && currentColumn + 1 < 7 && i != daysInMonth - 1) {
                            canvas.save();
                            canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                            canvas.drawRect(
                                cx,
                                cy - AndroidUtilities.dp(22),
                                cx + xStep / 2,
                                cy + AndroidUtilities.dp(22),
                                rangeSelectorPaint);
                            canvas.restore();
                        }
                    }
                    if(currentCellState == CellState.END) {
                        textPaint.setColor(Color.WHITE);
                        rangeSelectorPaint.setAlpha(255);
                        rangeSelectorPaint.setStyle(Paint.Style.FILL);

                        daysCount++;
                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(36) / 2, rangeSelectorPaint);

                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(36) / 2, rangeSelectorPaint);

                        rangeSelectorPaint.setStyle(Paint.Style.STROKE);
                        rangeSelectorPaint.setStrokeWidth(AndroidUtilities.dp(2));
                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2, rangeSelectorPaint);

                        rangeSelectorPaint.setAlpha(128);
                        rangeSelectorPaint.setStyle(Paint.Style.FILL);

                        if(!firstSelectedDay.equals(secondSelectedDay) && currentColumn != 0 && i != 0) {
                            clipOutPath.addCircle(cx, cy, AndroidUtilities.dp(42) / 2, Path.Direction.CW);

                            canvas.save();
                            canvas.clipPath(clipOutPath, Region.Op.DIFFERENCE);
                            canvas.drawRect(
                                cx - +xStep / 2,
                                cy - AndroidUtilities.dp(22),
                                cx,
                                cy + AndroidUtilities.dp(22),
                                rangeSelectorPaint);
                            canvas.restore();
                        }
                    }

                    canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
                    textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

                    DateCell dateCell = new DateCell();
                    dateCell.day = i + 1;
                    dateCell.month = currentMonthInYear;
                    dateCell.year = currentYear;
                    dateCell.drawRegion.set(cx - xStep / 2, cy - AndroidUtilities.dp(40), cx + xStep / 2, cy + AndroidUtilities.dp(40));
                    dateCells.add(dateCell);
                }

                currentColumn++;
                if (currentColumn >= 7) {
                    currentColumn = 0;
                    currentCell++;
                }

            }

            if(daysCount != 0) {
                actionBar.setTitle(LocaleController.formatPluralString("Days", daysCount));
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
            if (imagesByDays != null) {
                for (int i = 0; i < imagesByDays.size(); i++) {
                    imagesByDays.valueAt(i).onAttachedToWindow();
                }
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attached = false;
            if (imagesByDays != null) {
                for (int i = 0; i < imagesByDays.size(); i++) {
                    imagesByDays.valueAt(i).onDetachedFromWindow();
                }
            }
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onDateSelected(int startDate, int endDate, int daysCount);
    }

    private class PeriodDay {
        MessageObject messageObject;
        int startOffset;
        float enterAlpha = 1f;
        float startEnterDelay = 1f;
        boolean wasDrawn;
    }

    private class DateCell {
        private RectF drawRegion = new RectF();
        int year;
        int month;
        int day;

        public int compare(DateCell other) {
            if(this.equals(other)) {
                return 0;
            }

            if(year == other.year) {
                if(month == other.month) {
                    if(day == other.day) {
                        return 0;
                    } else if(day > other.day) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if(month > other.month) {
                    return 1;
                } else {
                    return -1;
                }
            } else if(year > other.year) {
                return 1;
            } else  {
                return -1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DateCell dateCell = (DateCell) o;
            return year == dateCell.year && month == dateCell.month && day == dateCell.day;
        }

        @Override
        public int hashCode() {
            return Objects.hash(year, month, day);
        }
    }


    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {

        ThemeDescription.ThemeDescriptionDelegate descriptionDelegate = new ThemeDescription.ThemeDescriptionDelegate() {
            @Override
            public void didSetColor() {
                updateColors();
            }
        };
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_windowBackgroundWhite);
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_windowBackgroundWhiteBlackText);
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_listSelector);


        return super.getThemeDescriptions();
    }

    @Override
    public boolean needDelayOpenAnimation() {
        return true;
    }

    @Override
    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        isOpened = true;
    }
}
