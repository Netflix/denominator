package denominator.example.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import denominator.Credentials;
import denominator.Provider;
import denominator.example.android.ui.PreferencesActivity;
import denominator.example.android.zone.ZoneListModule;
import denominator.ultradns.UltraDNSProvider;

import static android.widget.Toast.LENGTH_SHORT;

public class DenominatorApplication extends Application {

  private static final String TAG = "Denominator:Application";
  private ObjectGraph applicationGraph;

  @Override
  public void onCreate() {
    Log.i(TAG, " creating");
    super.onCreate();
    long start = System.currentTimeMillis();
    @Module(injects = DenominatorApplication.class, complete = false)
    class ApplicationModule {

      @Provides
      @Singleton
      Application application() {
        return DenominatorApplication.this;
      }
    }
    Object[] modulesForGraph = new Object[4];
    modulesForGraph[0] = new DenominatorProvider();
    modulesForGraph[1] = new CredentialsFromPreferencesModule();
    modulesForGraph[2] = new ApplicationModule();
    modulesForGraph[3] = new ZoneListModule();
    applicationGraph = ObjectGraph.create(modulesForGraph);
    long duration = System.currentTimeMillis() - start;
    String durationMessage = getString(R.string.init_duration, duration);
    Toast.makeText(this, durationMessage, LENGTH_SHORT).show();
    Log.i(TAG, " created");
  }

  public ObjectGraph getApplicationGraph() {
    return applicationGraph;
  }

  /**
   * Here's where to change for a different provider
   */
  @Module(includes = UltraDNSProvider.Module.class, complete = false)
  static final class DenominatorProvider {

    @Provides
    @Singleton
    public Provider provider() {
      return new UltraDNSProvider();
    }
  }

  @Module(
      injects = {
          PreferencesActivity.class
      },
      library = true,
      complete = false
  )
  static class CredentialsFromPreferencesModule {

    @Provides
    Credentials credentials(Application context, Provider provider) {
      PreferenceManager.getDefaultSharedPreferences(context);
      if (provider.credentialTypeToParameterNames().isEmpty()) {
        return Credentials.AnonymousCredentials.INSTANCE;
      }
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
      String credentialType = provider.credentialTypeToParameterNames().keySet().iterator().next();
      Map<String, String> creds = new LinkedHashMap<String, String>();
      for (String parameter : provider.credentialTypeToParameterNames().get(credentialType)) {
        String value = sp.getString(parameter, null);
        creds.put(parameter, value);
      }
      return Credentials.MapCredentials.from(creds);
    }
  }
}
