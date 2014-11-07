package ru.aim.anotheryetbashclient.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.aim.anotheryetbashclient.ActionsAndIntents;

import static ru.aim.anotheryetbashclient.loaders.Package.wrapWithRoot;
import static ru.aim.anotheryetbashclient.loaders.Package.wrapWithRootWithoutSlash;

/**
 *
 */
public class AbyssLoader extends RandomLoader {

    public static int ID = ActionsAndIntents.TYPE_ABYSS;

    static final String URL = wrapWithRoot("abyss");
    static final String MORE_URL = wrapWithRootWithoutSlash("%s");

    public AbyssLoader(Context context) {
        super(context);
    }

    @Override
    protected String getUrl() {
        if (!TextUtils.isEmpty(more)) {
            return String.format(MORE_URL, more);
        } else {
            return URL;
        }
    }

    protected void saveQuote(ContentValues values) {
        getDbHelper().addQuoteToDefault(values);
    }

    @Override
    protected Elements selectId(Element e) {
        return e.select("span[class=id]");
    }
}
