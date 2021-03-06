package site.zido.elise.select.configurable;

import site.zido.elise.http.Response;
import site.zido.elise.processor.ResultItem;

import java.util.List;
import java.util.Set;

/**
 * model extractor
 *
 * @author zido
 */
public interface ModelExtractor {

    /**
     * Extract result item.
     *
     * @param response the response
     * @return the result item
     */
    List<ResultItem> extract(Response response);

    /**
     * Extract links list.
     *
     * @param response the response
     * @return the list
     */
    Set<String> extractLinks(Response response);

    /**
     * get the task name
     *
     * @return task name
     */
    String getName();
}
