package nl.orhun.blogtojbake;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private final Connection connection = createConnection();
    private final String jbakeTemplate = readJbakeTemplate();

    public static void main(String[] args) {
        Main main = new Main();
        main.convert();
        main.closeConnection();
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(
                    "jdbc:mysql://orhun.nl:3306/orhunn1q_orhun?serverTimezone=Europe/Amsterdam",
                    System.getenv("dbuser"), System.getenv("dbpass"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String readJbakeTemplate() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("jbake_html_template.html")) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void convert() {
        List<Article> articlesWithCategories = getArticles().stream()
                .map(article -> article.toBuilder()
                        .categories(getCategoriesForArticle(article))
                        .build())
                .map(this::setInactiveIfContentIsEmpty)
                .map(this::prettifyPreTagsinContent)
                .toList();

        articlesWithCategories
                .forEach(this::createJbakeFiles);
    }

    private Article prettifyPreTagsinContent(Article article) {
        return article.toBuilder()
                .maincontent(article.getMaincontent().replace("<pre>", "<pre class=\"prettyprint\">"))
                .build();
    }

    private Article setInactiveIfContentIsEmpty(Article article) {
        if (article.getMaincontent() == null) {
            return article.toBuilder()
                    .maincontent("")
                    .inactive(1)
                    .build();
        }
        return article;
    }

    private List<Category> getCategoriesForArticle(Article article) {
        try (PreparedStatement statement = connection.prepareStatement("select * from article_cat ac inner join category c on ac.categoryid = c.systemid where ac.articleid = ?")) {
            statement.setInt(1, article.getSystemid());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (resultSet.next()) {
                    Category category = Category.builder()
                            .systemid(resultSet.getInt("systemid"))
                            .catname(resultSet.getString("catname"))
                            .catcolor(resultSet.getString("catcolor"))
                            .build();
                    categories.add(category);
                }
                return categories;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    List<Article> getArticles() {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "select * from article")) {
                List<Article> articles = new ArrayList<>();
                while (resultSet.next()) {
                    Article article = Article.builder()
                            .systemid(resultSet.getInt("systemid"))
                            .title(resultSet.getString("title"))
                            .inactive(resultSet.getInt("inactive"))
                            .publishdate(resultSet.getTimestamp("publishdate").toLocalDateTime())
                            .keywords(resultSet.getString("keywords"))
                            .friendlyurl(resultSet.getString("friendlyurl"))
                            .maincontent(resultSet.getString("maincontent"))
                            .build();
                    articles.add(article);
                }
                return articles;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createJbakeFiles(Article article) {
        String publishDate = article.getPublishdate().format(DateTimeFormatter.ISO_DATE);

        String jbakeArticle = jbakeTemplate.replace("%title%", article.getTitle())
                .replace("%date%", publishDate)
                .replace("%type%", "post")
                .replace("%tags%", article.getCategories().stream().map(Category::getCatname).collect(Collectors.joining(",")))
                .replace("%status%", article.getInactive() == 0 ? "published" : "draft")
                .replace("%maincontent%", article.getMaincontent());

        new File("target/jbakefiles").mkdir();

        Path articlePath = Path.of("target/jbakefiles", createFileName(article, publishDate));
        try {
            Files.writeString(articlePath, jbakeArticle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String createFileName(Article article, String publishDate) {
        String friendlyurl = article.getFriendlyurl();
        if (friendlyurl == null) {
            friendlyurl = article.getTitle();
        }
        String fileName = friendlyurl.replaceAll("[^a-z|A-Z0-9]", "-");
        return String.format("%s_%s.html", publishDate, fileName);
    }
}
