package denominator.example.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.TimeUnit;

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

  private ObjectGraph applicationGraph;

  @Override
  public void onCreate() {
    Log.i(TAG, " creating");
    super.onCreate();
    Stopwatch watch = new Stopwatch().start();
    @Module(injects = DenominatorApplication.class, complete = false)
    class ApplicationModule {
      @Provides
      @Singleton
      Application application() {
        return DenominatorApplication.this;
      }
    }
    Object[] modulesForGraph = ImmutableList.builder()
        .add(new DenominatorProvider())
        .add(new CredentialsFromPreferencesModule())
        .add(new ApplicationModule())
        .add(new ZoneListModule())
        .build().toArray();
    applicationGraph = ObjectGraph.create(modulesForGraph);
    long duration = watch.elapsed(TimeUnit.MILLISECONDS);
    String durationMessage = getString(R.string.init_duration, duration);
    Toast.makeText(this, durationMessage, LENGTH_SHORT).show();
    Log.i(TAG, " created");
  }

  public ObjectGraph getApplicationGraph() {
    return applicationGraph;
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
      String credentialType = provider.credentialTypeToParameterNames().keys().iterator().next();
      ImmutableMap.Builder<String, String> creds = ImmutableMap.builder();
      for (String parameter : provider.credentialTypeToParameterNames().get(credentialType)) {
        String value = sp.getString(parameter, null);
        creds.put(parameter, value);
      }
      return Credentials.MapCredentials.from(creds.build());
    }
  }
}
