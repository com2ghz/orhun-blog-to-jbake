package nl.orhun.blogtojbake;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
public class Article {
    private final int systemid;
    private final int inactive;
    private final LocalDateTime publishdate;
    private final String title;
    private final String keywords;
    private final String maincontent;
    private final String friendlyurl;
    private final List<Category> categories;
}
