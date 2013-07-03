package denominator.example.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import denominator.example.android.zone.ZoneList;
import denominator.model.Zone;

import static android.view.Gravity.CENTER;

public class ZoneListFragment extends Fragment {

  @Inject
  Activity activity;
  @Inject
  Bus bus;

  TableLayout zones;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    HomeActivity.class.cast(getActivity()).inject(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    zones = new TableLayout(getActivity());
    zones.setGravity(CENTER);
    ScrollView view = new ScrollView(getActivity());
    view.addView(zones);
    return view;
  }

  @Subscribe
  public void onZones(ZoneList.SuccessEvent event) {
    zones.removeAllViews();
    while (event.zones.hasNext()) {
      Zone zone = event.zones.next();
      TableRow row = new TableRow(activity);
      TextView name = new TextView(activity);
      name.setText(zone.name());
      row.addView(name);
      if (zone.id() != null) {
        TextView id = new TextView(activity);
        id.setText(zone.id());
        row.addView(id);
      }
      zones.addView(row);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    bus.register(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    bus.unregister(this);
  }
}
