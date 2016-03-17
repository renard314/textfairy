package com.renard.ocr.install;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.concurrent.ExecutionException;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 */
public class TaskFragment extends Fragment {


    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onPreExecute();

        void onProgressUpdate(int percent);

        void onCancelled();

        void onPostExecute(InstallResult result);
    }

    private InstallTask mTask;


    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onStart() {
        super.onStart();
        mTask.setTaskCallbacks((TaskCallbacks) getActivity());
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTask = new InstallTask((TaskCallbacks) getActivity(), getActivity().getAssets());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mTask.getStatus() == AsyncTask.Status.PENDING) {
            mTask.execute();
        }
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mTask.setTaskCallbacks(null);
    }

    @Nullable
    public InstallResult getInstallResult() {
        final AsyncTask.Status status = mTask.getStatus();
        if (status == AsyncTask.Status.FINISHED) {
            try {
                return mTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return new InstallResult(InstallResult.Result.UNSPECIFIED_ERROR);
        } else {
            return null;
        }
    }

}