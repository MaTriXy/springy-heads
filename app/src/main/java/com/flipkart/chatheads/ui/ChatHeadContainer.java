package com.flipkart.chatheads.ui;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import com.flipkart.chatheads.R;
import com.flipkart.chatheads.reboundextensions.ChatHeadSpringsHolder;
import com.flipkart.chatheads.reboundextensions.ChatHeadUtils;
import com.flipkart.chatheads.reboundextensions.ModifiedSpringChain;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ChatHeadContainer<T> extends FrameLayout {

    private static final int MAX_CHAT_HEADS = 5;
    private int maxWidth;
    private int maxHeight;
    private ChatHeadCloseButton closeButton;
    private ChatHeadSpringsHolder springsHolder;
    private MinimizedArrangement minimizedArrangement;
    private MaximizedArrangement maximizedArrangement;
    private ChatHeadArrangement activeArrangement;
    private Map<T, ChatHead> chatHeads = new LinkedHashMap<>();
    private ChatHeadViewAdapter viewAdapter;
    private View overlayView;

    public ChatHeadContainer(Context context) {
        super(context);
        init(context);
    }

    public ChatHeadContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatHeadContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ChatHeadCloseButton getCloseButton() {
        return closeButton;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public ChatHeadArrangement getActiveArrangement() {
        return activeArrangement;
    }

    public Map<T, ChatHead> getChatHeads() {
        return chatHeads;
    }

    void selectSpring(ChatHead chatHead) {
        springsHolder.selectSpring(chatHead);
        chatHead.bringToFront();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Selects the chat head. Very similar to performing touch up on it.
     *
     * @param chatHead
     */
    public void selectChatHead(ChatHead chatHead) {
        activeArrangement.selectChatHead(chatHead);
    }

    public void selectChatHead(T key) {
        ChatHead chatHead = chatHeads.get(key);
        selectChatHead(chatHead);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        maxWidth = getMeasuredWidth();
        maxHeight = getMeasuredHeight();

    }


    /**
     * Adds and returns the created chat head
     *
     * @param isSticky If sticky is true, then this chat head will never be auto removed when size exceeds.
     *                 Sticky chat heads can never be removed
     * @return
     */
    public ChatHead<T> addChatHead(T key, boolean isSticky) {
        final ChatHead<T> chatHead = new ChatHead(this, springsHolder, getContext(), isSticky);
        chatHead.setKey(key);
        chatHeads.put(key, chatHead);
        addView(chatHead);
        springsHolder.addChatHead(chatHead, chatHead, isSticky);
        if (springsHolder.getHorizontalSpringChain().getAllSprings().size() > MAX_CHAT_HEADS) {
            ModifiedSpringChain.SpringData oldestSpring = springsHolder.getOldestSpring(springsHolder.getHorizontalSpringChain(), true);
            ChatHead<T> chatHeadToRemove = (ChatHead) oldestSpring.getKey();
            removeChatHead(chatHeadToRemove.getKey());
        }
        reloadDrawable(key);
        springsHolder.selectSpring(chatHead);
        if (activeArrangement != null)
            activeArrangement.onChatHeadAdded(chatHead, springsHolder);

        return chatHead;
    }

    public void reloadDrawable(T key) {
        chatHeads.get(key).setImageDrawable(viewAdapter.getChatHeadDrawable(key));
    }


    public boolean removeChatHead(T key) {
        ChatHead chatHead = chatHeads.get(key);
        if (chatHead != null && chatHead.getParent() != null && !chatHead.isSticky()) {
            removeView(chatHead);
            chatHeads.remove(key);
            springsHolder.removeChatHead(chatHead);
            if (activeArrangement != null)
                activeArrangement.onChatHeadRemoved(chatHead, springsHolder);
            return true;
        }
        return false;
    }

    protected View getOverlayView() {
        return overlayView;
    }

    private void init(Context context) {
        springsHolder = new ChatHeadSpringsHolder();
        closeButton = new ChatHeadCloseButton(getContext());
        LayoutParams layoutParams = new LayoutParams(ChatHeadUtils.dpToPx(getContext(), 100), ChatHeadUtils.dpToPx(getContext(), 100));
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        layoutParams.bottomMargin = ChatHeadUtils.dpToPx(getContext(), 50);
        closeButton.setLayoutParams(layoutParams);
        addView(closeButton);
        minimizedArrangement = new MinimizedArrangement();
        maximizedArrangement = new MaximizedArrangement();
        setupOverlay(context);
        post(new Runnable() {
            @Override
            public void run() {
                setArrangement(maximizedArrangement);
            }
        });

    }

    private void setupOverlay(Context context) {
        overlayView = new View(context);
        overlayView.setBackgroundResource(R.drawable.overlay_transition);
        overlayView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleArrangement();
            }
        });
        addView(overlayView, 0);
    }


    double getDistanceCloseButtonFromHead(float touchX, float touchY) {
        int left = closeButton.getLeft();
        int top = closeButton.getTop();
        double xDiff = touchX - left - closeButton.getTranslationX() - closeButton.getMeasuredWidth() / 2;
        double yDiff = touchY - top - closeButton.getTranslationY() - closeButton.getMeasuredHeight() / 2;
        double distance = Math.hypot(xDiff, yDiff);
        return distance;
    }

    void captureChatHeads(ChatHead causingChatHead) {
        activeArrangement.onCapture(this, causingChatHead);
    }

    public void removeAllChatHeads() {
        Set<Map.Entry<T, ChatHead>> entries = chatHeads.entrySet();
        Iterator<Map.Entry<T, ChatHead>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            removeChatHead(iterator.next().getKey());
        }
    }

    private void setArrangement(final ChatHeadArrangement arrangement) {
        if (activeArrangement != null && arrangement != activeArrangement) {
            activeArrangement.onDeactivate(maxWidth, maxHeight, springsHolder.getActiveHorizontalSpring(), springsHolder.getActiveVerticalSpring());
        }
        activeArrangement = arrangement;

        arrangement.onActivate(ChatHeadContainer.this, springsHolder, maxWidth, maxHeight);
        if (arrangement == maximizedArrangement) {
            showOverlayView();
        } else {
            hideOverlayView();
        }
    }

    private void hideOverlayView() {
        TransitionDrawable drawable = (TransitionDrawable) overlayView.getBackground();
        drawable.reverseTransition(200);
        overlayView.setClickable(false);
    }

    private void showOverlayView() {
        TransitionDrawable drawable = (TransitionDrawable) overlayView.getBackground();
        drawable.startTransition(200);
        overlayView.setClickable(true);

    }

    public void toggleArrangement() {
        if (activeArrangement == maximizedArrangement) {
            setArrangement(minimizedArrangement);
        } else {
            setArrangement(maximizedArrangement);
        }
    }

    public int[] getChatHeadCoordsForCloseButton(ChatHead chatHead) {
        int[] coords = new int[2];
        int x = (int) (closeButton.getLeft() + closeButton.getTranslationX() + closeButton.getMeasuredWidth() / 2 - chatHead.getMeasuredWidth() / 2);
        int y = (int) (closeButton.getTop() + closeButton.getTranslationY() + closeButton.getMeasuredHeight() / 2 - chatHead.getMeasuredHeight() / 2);
        coords[0] = x;
        coords[1] = y;
        return coords;
    }

    public void setViewAdapter(ChatHeadViewAdapter chatHeadViewAdapter) {
        minimizedArrangement.setViewAdapter(chatHeadViewAdapter);
        maximizedArrangement.setViewAdapter(chatHeadViewAdapter);
        this.viewAdapter = chatHeadViewAdapter;
    }
}