package ru.aim.anotheryetbashclient.helper.actions;

import android.content.Context;
import android.os.Bundle;

import org.apache.http.client.HttpClient;

import ru.aim.anotheryetbashclient.BashApplication;
import ru.aim.anotheryetbashclient.helper.DbHelper;

public class ActionFactory {

    final Context context;
    final DbHelper dbHelper;
    final HttpClient httpClient;

    public ActionFactory(Context context) {
        this.context = context;
        dbHelper = new DbHelper(context);
        BashApplication bashApplication = (BashApplication) context.getApplicationContext();
        httpClient = bashApplication.getHttpClient();
    }

    public <T extends IAction> T build(Class<T> clazz, Bundle arguments) {
        T action;
        try {
            action = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (action instanceof IContextAware) {
            ((IContextAware) action).setContext(context);
        }
        if (action instanceof IDbAware) {
            ((IDbAware) action).setDbHelper(dbHelper);
        }
        if (action instanceof IHttpClientAware) {
            ((IHttpClientAware) action).setHttpClient(httpClient);
        }
        if (action instanceof IBundleAware) {
            ((IBundleAware) action).setBundle(arguments);
        }
        return action;
    }

    public <T extends IAction> T build(Class<T> clazz) {
        return build(clazz, Bundle.EMPTY);
    }
}
