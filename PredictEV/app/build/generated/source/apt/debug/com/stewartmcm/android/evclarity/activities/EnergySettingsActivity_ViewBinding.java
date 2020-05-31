// Generated code from Butter Knife. Do not modify!
package com.stewartmcm.android.evclarity.activities;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import com.stewartmcm.android.evclarity.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class EnergySettingsActivity_ViewBinding implements Unbinder {
  private EnergySettingsActivity target;

  private View view2131624059;

  @UiThread
  public EnergySettingsActivity_ViewBinding(EnergySettingsActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public EnergySettingsActivity_ViewBinding(final EnergySettingsActivity target, View source) {
    this.target = target;

    View view;
    target.currentUtilityTextView = Utils.findRequiredViewAsType(source, R.id.current_utility_text_view, "field 'currentUtilityTextView'", TextView.class);
    target.utilityRateTextView = Utils.findRequiredViewAsType(source, R.id.utility_rate_text_view, "field 'utilityRateTextView'", TextView.class);
    target.gasPriceEditText = Utils.findRequiredViewAsType(source, R.id.gas_price_edit_text, "field 'gasPriceEditText'", EditText.class);
    target.mpgEditText = Utils.findRequiredViewAsType(source, R.id.mpg_edit_text, "field 'mpgEditText'", EditText.class);
    view = Utils.findRequiredView(source, R.id.fab, "method 'findUtilities'");
    view2131624059 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.findUtilities();
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    EnergySettingsActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.currentUtilityTextView = null;
    target.utilityRateTextView = null;
    target.gasPriceEditText = null;
    target.mpgEditText = null;

    view2131624059.setOnClickListener(null);
    view2131624059 = null;
  }
}
