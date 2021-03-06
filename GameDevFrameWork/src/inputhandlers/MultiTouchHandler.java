package inputhandlers;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;
import android.view.View;
import interfaces.Input;
import interfaces.Input.TouchEvent;
import interfaces.TouchHandler;
import inputhandlers.Pool.PoolObjectFactory;

public class MultiTouchHandler implements TouchHandler {

	final static private int numPointers = 20;
    boolean[] isTouched = new boolean[numPointers];
    int[] touchX = new int[numPointers];
    int[] touchY = new int[numPointers];
    Pool<TouchEvent> touchEventPool;

    List<TouchEvent> touchEvents = new ArrayList<TouchEvent>();
    List<TouchEvent> touchEventsBuffer = new ArrayList<TouchEvent>();
    float scaleX;
    float scaleY;

    public MultiTouchHandler(View view, float scaleX, float scaleY) {
        PoolObjectFactory<TouchEvent>factory= new PoolObjectFactory<Input.TouchEvent>() {

			@Override
			public TouchEvent createObject() {
				// TODO Auto-generated method stub
				return new Input.TouchEvent();
			}
		};
    	touchEventPool = new Pool<Input.TouchEvent>(factory, 100);
        view.setOnTouchListener(this);

        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        synchronized (this) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            int pointerId = event.getPointerId(pointerIndex);
            TouchEvent touchEvent;

            switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touchEvent = touchEventPool.newObject();
                touchEvent.type = TouchEvent.TOUCH_DOWN;
                touchEvent.pointer = pointerId;
                touchEvent.x = touchX[pointerId] = (int) (event.getX(pointerIndex) * scaleX);
                touchEvent.y = touchY[pointerId] = (int) (event.getY(pointerIndex) * scaleY);
                isTouched[pointerId] = true;
                touchEventsBuffer.add(touchEvent);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                touchEvent = touchEventPool.newObject();
                touchEvent.type = TouchEvent.TOUCH_UP;
                touchEvent.pointer = pointerId;
                touchEvent.x = touchX[pointerId] = (int) (event.getX(pointerIndex) * scaleX);
                touchEvent.y = touchY[pointerId] = (int) (event.getY(pointerIndex) * scaleY);
                isTouched[pointerId] = false;
                touchEventsBuffer.add(touchEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerCount = event.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    pointerIndex = i;
                    pointerId = event.getPointerId(pointerIndex);
                    touchEvent = touchEventPool.newObject();
                    touchEvent.type = TouchEvent.TOUCH_DRAG;
                    touchEvent.pointer = pointerId;
                    touchEvent.x = touchX[pointerId] = (int) (event.getX(pointerIndex) * scaleX);
                    touchEvent.y = touchY[pointerId] = (int) (event.getY(pointerIndex) * scaleY);
                    touchEventsBuffer.add(touchEvent);
                }
                break;
            }
            return true;
        }
    }

    public boolean isTouchDown(int pointer) {
        synchronized (this) {
            if (pointer < 0 || pointer >= numPointers) {
                return false;
            }
            return isTouched[pointer];
        }
    }

    public int getTouchX(int pointer) {
        synchronized (this) {
            if (pointer < 0 || pointer >= numPointers) {
                return 0;
            }
            return touchX[pointer];
        }
    }

    public int getTouchY(int pointer) {
        synchronized (this) {
            if (pointer < 0 || pointer >= numPointers) {
                return 0;
            }
            return touchY[pointer];
        }
    }

    public List<TouchEvent> getTouchEvents() {
        synchronized (this) {
            final int len = touchEvents.size();
            for (int i = 0; i < len; i++) {
                touchEventPool.free(touchEvents.get(i));
            }
            touchEvents.clear();
            touchEvents.addAll(touchEventsBuffer);
            touchEventsBuffer.clear();
            return touchEvents;
        }
    }
}
