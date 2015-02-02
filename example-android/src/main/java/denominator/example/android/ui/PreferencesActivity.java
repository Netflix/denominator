package denominator.example.android.ui;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import javax.inject.Inject;

import denominator.Provider;
import denominator.example.android.DenominatorApplication;

public class PreferencesActivity extends PreferenceActivity {

  @Inject
  Provider provider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DenominatorApplication.class.cast(getApplication()).getApplicationGraph().inject(this);
    // to support api level 9
    this.setPreferenceScreen(createFromProvider());
  }

  private PreferenceScreen createFromProvider() {
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
    if (provider.credentialTypeToParameterNames().isEmpty()) {
      return root;
    }
    String credentialType = provider.credentialTypeToParameterNames().keySet().iterator().next();
    root.setTitle(credentialType + " credentials for provider " + provider.name());
    for (String parameter : provider.credentialTypeToParameterNames().get(credentialType)) {
      EditTextPreference cred = new EditTextPreference(this);
      cred.setKey(parameter);
      cred.setTitle(parameter);
      cred.setDialogTitle(parameter);
      root.addPreference(cred);
    }
    return root;
  }
}
