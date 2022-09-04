package nl.orhun.blogtojbake;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Category {
    private final int systemid;
    private final String catname;
    private final String catcolor;
}
