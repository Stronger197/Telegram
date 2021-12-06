package org.telegram.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.EmojiData;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.StickerSetBulletinLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class ReactionsAnimationsOverlay {


    ChatActivity chatActivity;
    int currentAccount;

    ArrayList<DrawingObject> drawingObjects = new ArrayList<>();

    FrameLayout contentLayout;
    RecyclerListView listView;
    long dialogId;
    int threadMsgId;


    public ReactionsAnimationsOverlay(ChatActivity chatActivity, FrameLayout frameLayout, RecyclerListView chatListView, int currentAccount, long dialogId, int threadMsgId) {
        this.chatActivity = chatActivity;
        this.contentLayout = frameLayout;
        this.listView = chatListView;
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.threadMsgId = threadMsgId;
    }

    protected void onAttachedToWindow() {
    }

    protected void onDetachedFromWindow() {
    }


    public void draw(Canvas canvas) {
        if (!drawingObjects.isEmpty()) {
            for (int i = 0; i < drawingObjects.size(); i++) {

                DrawingObject drawingObject = drawingObjects.get(i);

                drawingObject.viewFound = false;

                float scaleFactor = 0.2f;
                if(drawingObject.imageReceiver.getLottieAnimation() != null) {
                    int currentFrame = drawingObject.imageReceiver.getLottieAnimation().getCurrentFrame();
                    int totalFrame = drawingObject.imageReceiver.getLottieAnimation().getFramesCount();
                    float animationFrameCount = totalFrame / 10f;
                    float currentStep = currentFrame / animationFrameCount;
                    if(currentStep > 1) {
                        currentStep = 1;
                    }
                    scaleFactor += 0.8f * currentStep;

                    float lastFrames = totalFrame - animationFrameCount;
                    float lastFramesCount = animationFrameCount - lastFrames;
                    if(currentFrame >= lastFrames) {
                        float minStep = -((currentFrame - lastFrames) / lastFramesCount) * 10;
                        if(minStep > 1) {
                            minStep = 1;
                        }
                        scaleFactor -= 0.8f * minStep;
                    }
                }


                for (int k = 0; k < listView.getChildCount(); k++) {
                    View child = listView.getChildAt(k);
                    if (child instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) child;
                        if (cell.getMessageObject().getId() == drawingObject.messageId) {
                            drawingObject.viewFound = true;

                            drawingObject.lastX = drawingObject.randomOffsetX - drawingObject.lastW / 2;
                            drawingObject.lastY = listView.getY() + child.getY() + drawingObject.randomOffsetY - drawingObject.lastW / 2;

                            break;
                        }
                    }
                }

                drawingObject.imageReceiver.setImageCoords(
                    drawingObject.lastX + (drawingObject.lastW * (1 - scaleFactor) / 2),
                    drawingObject.lastY + (drawingObject.lastW * (1 - scaleFactor) / 2),
                    drawingObject.lastW * scaleFactor,
                    drawingObject.lastW * scaleFactor
                );


                if (!drawingObject.isOut) {
                    canvas.save();
                    canvas.scale(-1f, 1, drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
                    drawingObject.imageReceiver.draw(canvas);
                    canvas.restore();
                } else {
                    drawingObject.imageReceiver.draw(canvas);
                }
                if (drawingObject.wasPlayed && drawingObject.imageReceiver.getLottieAnimation() != null && drawingObject.imageReceiver.getLottieAnimation().getCurrentFrame() == drawingObject.imageReceiver.getLottieAnimation().getFramesCount() - 2) {
                    drawingObjects.remove(i);
                    i--;
                } else if (drawingObject.imageReceiver.getLottieAnimation() != null && drawingObject.imageReceiver.getLottieAnimation().isRunning()) {
                    drawingObject.wasPlayed = true;
                } else if (drawingObject.imageReceiver.getLottieAnimation() != null && !drawingObject.imageReceiver.getLottieAnimation().isRunning()) {
                    drawingObject.imageReceiver.getLottieAnimation().setCurrentFrame(0, true);
                    drawingObject.imageReceiver.getLottieAnimation().start();
                }
            }
            contentLayout.invalidate();
        }
    }

    public void onTapReaction(ChatMessageCell view, TLRPC.TL_availableReaction reaction) {
        onTapReaction(view, reaction, 0f, 0f);
    }

    public void onTapReaction(ChatMessageCell view, TLRPC.TL_availableReaction reaction, float x, float y) {
        if(view == null || view.getMessageObject() == null || view.getMessageObject().getId() < 0) {
            return;
        }

        showAnimationForReaction(view, -1, reaction, x, y);
        showAnimationForReaction2(view, -1, reaction, x, y);
    }


    private boolean showAnimationForReaction(ChatMessageCell view, int animation, TLRPC.TL_availableReaction reaction, float x, float y) {
        if (drawingObjects.size() > 12) {
            return false;
        }

        if (reaction == null) {
            return false;
        }
        float imageH = 280;
        float imageW = 280;
        if (imageH <= 0 || imageW <= 0) {
            return false;
        }

        if (true) {
            TLRPC.Document document = reaction.effect_animation;

            DrawingObject drawingObject = new DrawingObject();
            drawingObject.randomOffsetX = x;
            drawingObject.randomOffsetY = y;
            drawingObject.messageId = view.getMessageObject().getId();
            drawingObject.document = document;
            drawingObject.isOut = view.getMessageObject().isOutOwner();


            ImageLocation imageLocation = ImageLocation.getForDocument(document);
            int w = (int) (2f * imageW / AndroidUtilities.density);
            if(reaction.reaction.equals("\uD83E\uDD29")) {
                drawingObject.imageReceiver.setImage(imageLocation, "50_50", null, "tgs", drawingObject, 1);
            } else {
                drawingObject.imageReceiver.setImage(imageLocation, null, null, "tgs", drawingObject, 1);
            }
            drawingObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
            drawingObject.imageReceiver.setAllowStartAnimation(true);
            drawingObject.imageReceiver.setAutoRepeat(0);
            drawingObject.lastW = imageW * 3;
            if (drawingObject.imageReceiver.getLottieAnimation() != null) {
                drawingObject.imageReceiver.getLottieAnimation().start();
            }
            drawingObjects.add(drawingObject);
            drawingObject.imageReceiver.onAttachedToWindow();
            drawingObject.imageReceiver.setParentView(contentLayout);
            contentLayout.invalidate();

            return true;
        }
        return false;
    }


    private boolean showAnimationForReaction2(ChatMessageCell view, int animation, TLRPC.TL_availableReaction reaction, float x, float y) {
        if (drawingObjects.size() > 12) {
            return false;
        }

        if (reaction == null) {
            return false;
        }

        float imageH = 128;
        float imageW = 128;
        if (imageH <= 0 || imageW <= 0) {
            return false;
        }

        if (true) {
            TLRPC.Document document = reaction.activate_animation;

            DrawingObject drawingObject = new DrawingObject();
            drawingObject.randomOffsetX = x;
            drawingObject.randomOffsetY = y;
            drawingObject.messageId = view.getMessageObject().getId();
            drawingObject.document = document;
            drawingObject.isOut = view.getMessageObject().isOutOwner();

            ImageLocation imageLocation = ImageLocation.getForDocument(document);
            int w = (int) (2f * imageW / AndroidUtilities.density);
            if(reaction.reaction.equals("\uD83E\uDD29")) {
                drawingObject.imageReceiver.setImage(imageLocation, "50_50", null, "tgs", drawingObject, 1);
            } else {
                drawingObject.imageReceiver.setImage(imageLocation, null, null, "tgs", drawingObject, 1);
            }
            drawingObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
            drawingObject.imageReceiver.setAllowStartAnimation(true);
            drawingObject.imageReceiver.setAutoRepeat(0);
            if (drawingObject.imageReceiver.getLottieAnimation() != null) {
                drawingObject.imageReceiver.getLottieAnimation().start();
            }
            drawingObject.lastW = imageW * 3;
            drawingObjects.add(drawingObject);
            drawingObject.imageReceiver.onAttachedToWindow();
            drawingObject.imageReceiver.setParentView(contentLayout);
            contentLayout.invalidate();

            return true;
        }
        return false;
    }


    public void onScrolled(int dy) {
        for (int i = 0; i < drawingObjects.size(); i++) {
            if (!drawingObjects.get(i).viewFound) {
                drawingObjects.get(i).lastY -= dy;
            }
        }
    }

    private class DrawingObject {
        public float lastX;
        public float lastY;
        public boolean viewFound;
        public float lastW;
        public float randomOffsetX;
        public float randomOffsetY;
        boolean wasPlayed;
        boolean isOut;
        int messageId;
        TLRPC.Document document;
        ImageReceiver imageReceiver = new ImageReceiver();
    }
}