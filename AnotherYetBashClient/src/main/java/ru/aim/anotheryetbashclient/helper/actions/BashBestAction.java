package ru.aim.anotheryetbashclient.helper.actions;

import static ru.aim.anotheryetbashclient.helper.actions.Package.wrapWithRoot;

@SuppressWarnings("unused")
public class BashBestAction extends AbstractAction {

    public static final String TAG = "BashBestAction";

    static final String URL = wrapWithRoot("best");

    @Override
    protected String getUrl() {
        return URL;
    }
}
