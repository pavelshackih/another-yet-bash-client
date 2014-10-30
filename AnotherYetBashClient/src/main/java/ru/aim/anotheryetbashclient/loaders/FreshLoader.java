package ru.aim.anotheryetbashclient.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;

import ru.aim.anotheryetbashclient.ActionsAndIntents;
import ru.aim.anotheryetbashclient.BashApplication;
import ru.aim.anotheryetbashclient.R;
import ru.aim.anotheryetbashclient.SettingsHelper;
import ru.aim.anotheryetbashclient.helper.DbHelper;
import ru.aim.anotheryetbashclient.helper.L;
import ru.aim.anotheryetbashclient.helper.Utils;

import static ru.aim.anotheryetbashclient.helper.DbHelper.QUOTE_PUBLIC_ID;
import static ru.aim.anotheryetbashclient.loaders.Package.getCharsetFromResponse;
import static ru.aim.anotheryetbashclient.loaders.Package.wrapWithRoot;

/**
 *
 */
public class FreshLoader extends AbstractLoader<FreshResult> {

    public static final String ROOT_PAGE = wrapWithRoot("");
    public static final String NEXT_PAGE = wrapWithRoot("index/%s");
    boolean fromService;

    public static final int ID = ActionsAndIntents.TYPE_NEW;

    int mCurrentPage;

    public FreshLoader(Context context, Bundle bundle) {
        super(context);
        mCurrentPage = bundle.getInt(ActionsAndIntents.CURRENT_PAGE, 0);
    }

    @Override
    public FreshResult doInBackground() throws Exception {
        if (Utils.isNetworkNotAvailable(getContext())) {
            throw new RuntimeException(getContext().getString(R.string.error_no_connection));
        }
        FreshResult result = new FreshResult();
        String url = getUrl();

        HttpGet httpRequest = new HttpGet(url);
        BashApplication app = (BashApplication) getContext().getApplicationContext();
        HttpResponse httpResponse = app.getHttpClient().execute(httpRequest);
        Document document = Jsoup.parse(httpResponse.getEntity().getContent(), getCharsetFromResponse(httpResponse), url);
        if (mCurrentPage != -1) {
            Elements elements = document.select("input[class=page]");
            String page = null;
            for (Element e : elements) {
                page = e.attr("value");
            }
            if (!TextUtils.isEmpty(page)) {
                result.currentPage = Integer.parseInt(page);
                result.maxPage = result.currentPage;
            }
        }
        Elements quotesElements = document.select("div[class=quote]");
        L.d(TAG, "Quotes size " + quotesElements.size());
        getDbHelper().clearDefault();
        if (!fromService && isFirstPage()) {
            getDbHelper().clearOffline();
        }
        for (Element e : quotesElements) {
            Elements idElements = e.select("a[class=id]");
            Elements dateElements = e.select("span[class=date]");
            Elements textElements = e.select("div[class=text]");
            Elements ratingElements = e.select("span[class=rating]");
            if (!textElements.isEmpty()) {
                String id = idElements.html();
                if (getDbHelper().notExists(id)) {
                    ContentValues values = new ContentValues();
                    values.put(QUOTE_PUBLIC_ID, idElements.html());
                    values.put(DbHelper.QUOTE_DATE, dateElements.html());
                    values.put(DbHelper.QUOTE_IS_NEW, 1);
                    values.put(DbHelper.QUOTE_RATING, ratingElements.html().trim());
                    values.put(DbHelper.QUOTE_TEXT, textElements.html().trim());
                    L.d(TAG, "Insert new item: " + values);
                    addQuote(values);
                    if (!fromService && isFirstPage()) {
                        getDbHelper().addQuoteToOffline(values);
                    }
                }
            }
        }
        if (!fromService && isFirstPage()) {
            SettingsHelper.writeTimestamp(getContext(), Calendar.getInstance().getTimeInMillis());
        }
        result.cursor = getDbHelper().selectFromDefaultTable();
        return result;
    }

    protected void addQuote(ContentValues contentValues) {
        getDbHelper().addQuoteToDefault(contentValues);
    }

    boolean isFirstPage() {
        return mCurrentPage == 0;
    }

    public void setFromService(boolean fromService) {
        this.fromService = fromService;
    }

    protected String getUrl() {
        if (isFirstPage()) {
            return ROOT_PAGE;
        } else {
            return String.format(NEXT_PAGE, mCurrentPage);
        }
    }
}
