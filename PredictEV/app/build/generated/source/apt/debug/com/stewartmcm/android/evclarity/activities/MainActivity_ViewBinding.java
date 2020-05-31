// Generated code from Butter Knife. Do not modify!
package com.stewartmcm.android.evclarity.activities;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.stewartmcm.android.evclarity.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class MainActivity_ViewBinding implements Unbinder {
  private MainActivity target;

  @UiThread
  public MainActivity_ViewBinding(MainActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public MainActivity_ViewBinding(MainActivity target, View source) {
    this.target = target;

    target.errorTextView = Utils.findRequiredViewAsType(source, R.id.error, "field 'errorTextView'", TextView.class);
    target.noTripsYetTextView = Utils.findRequiredViewAsType(source, R.id.recyclerview_triplog_empty, "field 'noTripsYetTextView'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    MainActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.errorTextView = null;
    target.noTripsYetTextView = null;
  }
}
