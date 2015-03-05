package imac.supernova;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import imac.supernova.datamodel.Game;
import imac.supernova.datamodel.ship.Ship;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link //FleetDashboardFragment.//OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FleetDashboardFragment#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class FleetDashboardFragment extends Fragment implements View.OnClickListener {

    private View v;

    private TextView ship_name;

    private TextView health_units;
    private TextView power_units;
    private TextView move_units;

    private RelativeLayout.LayoutParams params_health;
    private RelativeLayout.LayoutParams params_power;
    private RelativeLayout.LayoutParams params_move;

    public Game currentGame;
    //private Player currentPlayer = game.getPlayer(0);
    public List<Ship> playerFleet;

    public FleetDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Get the current game and the current player's fleet
        currentGame = ((MainActivity) getActivity()).game;
        playerFleet = currentGame.getFleetOfCurrentPlayer();

        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_fleet_dashboard, container, false);
        // Get textview to insert data
        ship_name = (TextView) v.findViewById(R.id.ship_name);
        health_units = (TextView) v.findViewById(R.id.health_units);
        power_units = (TextView) v.findViewById(R.id.power_units);
        move_units = (TextView) v.findViewById(R.id.move_units);

        // Button #1
        Button button_cruiser = (Button) v.findViewById(R.id.button_cruiser);
        button_cruiser.setOnClickListener(this);

        // Button #2
        Button button_bomber_1 = (Button) v.findViewById(R.id.button_bomber_1);
        button_bomber_1.setOnClickListener(this);
        // Button #3
        Button button_bomber_2 = (Button) v.findViewById(R.id.button_bomber_2);
        button_bomber_2.setOnClickListener(this);

        // Button #4
        Button button_fighter_1 = (Button) v.findViewById(R.id.button_fighter_1);
        button_fighter_1.setOnClickListener(this);
        // Button #5
        Button button_fighter_2 = (Button) v.findViewById(R.id.button_fighter_2);
        button_fighter_2.setOnClickListener(this);
        // Button #6
        Button button_fighter_3 = (Button) v.findViewById(R.id.button_fighter_3);
        button_fighter_3.setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_cruiser:
                updateDisplay(0);
                break;
            case R.id.button_bomber_1:
                updateDisplay(1);
                break;
            case R.id.button_bomber_2:
                updateDisplay(2);
                break;
            case R.id.button_fighter_1:
                updateDisplay(3);
                break;
            case R.id.button_fighter_2:
                updateDisplay(4);
                break;
            case R.id.button_fighter_3:
                updateDisplay(5);
                break;
            default:
                break;
        }
    }

    /**
     * Update the display of ship's data on click on buttons
     * */
    public void updateDisplay(int id) {
        ship_name.setText(playerFleet.get(id).getClass().getSimpleName().toString());
        // Health
        params_health = new RelativeLayout.LayoutParams(playerFleet.get(id).getHealth(), RelativeLayout.LayoutParams.MATCH_PARENT);
        health_units.setLayoutParams(params_health);
        // Power
        params_power = new RelativeLayout.LayoutParams(250, RelativeLayout.LayoutParams.MATCH_PARENT);
        power_units.setLayoutParams(params_power);
        // Move
        params_move = new RelativeLayout.LayoutParams(250, RelativeLayout.LayoutParams.MATCH_PARENT);
        move_units.setLayoutParams(params_move);
    }

}
