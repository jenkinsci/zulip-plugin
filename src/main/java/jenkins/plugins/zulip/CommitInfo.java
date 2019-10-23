package jenkins.plugins.zulip;

/**
 * Commit decscription useful for holding parsed commits and sending private notifications
 * to users using email as Zulip user ID.
 */
public class CommitInfo {
    public final String author;
    public final String email;
    public final String message;

    public CommitInfo(String author, String email, String message) {
        this.author = author;
        this.email = email;
        this.message = message;
    }
}
