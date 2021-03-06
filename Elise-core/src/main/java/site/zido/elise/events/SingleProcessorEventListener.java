package site.zido.elise.events;

import site.zido.elise.processor.ResultItem;

import java.util.EventListener;

public interface SingleProcessorEventListener extends EventListener {
    /**
     * On save success.
     *
     * @param resultItems the result items
     */
    default void onSaveSuccess(ResultItem resultItems) {
    }

    /**
     * On save error.
     *
     * @param resultItems the result items
     */
    default void onSaveError(ResultItem resultItems) {
    }
}
