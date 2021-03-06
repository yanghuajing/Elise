package site.zido.elise.select.configurable;

import site.zido.elise.select.Selector;

public class DefaultFieldExtractor implements FieldExtractor {
    /**
     * 字段名
     */
    private String name;
    /**
     * 是否允许空 ,默认允许为空
     */
    private Boolean nullable = true;

    /**
     * 选择抽取范围
     */
    private Source source = Source.REGION;

    /**
     * 抓取器
     */
    private Selector selector;

    /**
     * Instantiates a new Def extractor.
     */
    public DefaultFieldExtractor() {
    }


    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     * @return the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets nullable.
     *
     * @return the nullable
     */
    public boolean getNullable() {
        return nullable;
    }

    /**
     * Sets nullable.
     *
     * @param nullable the nullable
     * @return the nullable
     */
    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Gets source.
     *
     * @return the source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Sets source.
     *
     * @param source the source
     * @return the source
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * Compile selector selector.
     *
     * @return the selector
     */
    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }
}
