package org.slavick.dailydozen.widget;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.joanzapata.iconify.widget.IconTextView;

import org.slavick.dailydozen.Args;
import org.slavick.dailydozen.R;
import org.slavick.dailydozen.activity.FoodHistoryActivity;
import org.slavick.dailydozen.activity.FoodInfoActivity;
import org.slavick.dailydozen.model.Food;
import org.slavick.dailydozen.model.Servings;
import org.slavick.dailydozen.task.CalculateStreakTask;
import org.slavick.dailydozen.task.StreakTaskInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FoodServings extends RecyclerView.ViewHolder implements CalculateStreakTask.Listener {
    private final static String TAG = FoodServings.class.getSimpleName();

    private Date date;
    private Food food;

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    private TextView tvName;
    private StreakWidget tvStreak;
    private ViewGroup vgCheckboxes;
    private IconTextView ivFoodHistory;

    private ClickListener listener;

    public FoodServings(View itemView) {
        super(itemView);

        tvName = (TextView) itemView.findViewById(R.id.food_name);
        tvStreak = (StreakWidget) itemView.findViewById(R.id.food_streak);
        vgCheckboxes = (ViewGroup) itemView.findViewById(R.id.food_checkboxes);
        ivFoodHistory = (IconTextView) itemView.findViewById(R.id.food_history);
    }

    private Context getContext() {
        return itemView.getContext();
    }

    public void setDateAndFood(final Date date, final Food food) {
        this.date = date;
        this.food = food;

        initFoodName();
        initFoodHistory();

        final Servings servings = getServings();
        initCheckboxes(servings);
        initFoodStreak(servings);
    }

    private void initFoodName() {
        tvName.setText(String.format("%s %s", food.getName(), getContext().getString(R.string.icon_info)));

        tvName.setOnClickListener(getOnFoodNameClickListener());
    }

    private void initFoodStreak(Servings servings) {
        tvStreak.setStreak(servings != null ? servings.getStreak() : 0);
    }

    private View.OnClickListener getOnFoodNameClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(createFoodIntent(FoodInfoActivity.class, food));
            }
        };
    }

    private void initFoodHistory() {
        ivFoodHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(createFoodIntent(FoodHistoryActivity.class, food));
            }
        });
    }

    private Intent createFoodIntent(final Class<? extends AppCompatActivity> klass, final Food food) {
        final Intent intent = new Intent(getContext(), klass);
        intent.putExtra(Args.FOOD_ID, food.getId());
        return intent;
    }

    private void initCheckboxes(Servings servings) {
        int numExistingServings = servings != null ? servings.getServings() : 0;

        final List<CheckBox> checkBoxes = new ArrayList<>();

        for (int i = 0; i < food.getRecommendedServings(); i++) {
            final boolean isChecked = numExistingServings > 0;

            checkBoxes.add(createCheckBox(isChecked));

            numExistingServings--;
        }

        vgCheckboxes.removeAllViews();

        // Add the checkboxes in reverse order because they were checked from left to right. Reversing the order
        // makes it so the checks appear right to left.
        Collections.reverse(checkBoxes);
        for (CheckBox checkBox : checkBoxes) {
            vgCheckboxes.addView(checkBox);
        }
    }

    private CheckBox createCheckBox(final boolean isChecked) {
        final CheckBox checkBox = new CheckBox(itemView.getContext());

        // It is necessary to set the checked status before we set the onCheckedChangeListener
        checkBox.setChecked(isChecked);

        checkBox.setOnCheckedChangeListener(getOnCheckedChangeListener());

        return checkBox;
    }

    private CompoundButton.OnCheckedChangeListener getOnCheckedChangeListener() {
        if (onCheckedChangeListener == null) {
            onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        handleServingChecked();
                    } else {
                        handleServingUnchecked();
                    }
                }
            };
        }

        return onCheckedChangeListener;
    }

    private void handleServingChecked() {
        final Servings servings = Servings.createServingsIfDoesNotExist(date, food);
        if (servings != null) {
            servings.increaseServings();
            servings.save();

            onServingsChanged();

            Log.d(TAG, String.format("Increased Servings for %s", servings));
        }
    }

    private void handleServingUnchecked() {
        final Servings servings = getServings();
        if (servings != null) {
            servings.decreaseServings();

            if (servings.getServings() > 0) {
                servings.save();

                Log.d(TAG, String.format("Decreased Servings for %s", servings));
            } else {
                Log.d(TAG, String.format("Deleting %s", servings));
                servings.delete();
            }

            onServingsChanged();
        }
    }

    private void onServingsChanged() {
        new CalculateStreakTask(getContext(), this).execute(new StreakTaskInput(date, food));

        if (listener != null) {
            listener.onServingsChanged();
        }
    }

    public void setListener(ClickListener listener) {
        this.listener = listener;
    }

    private Servings getServings() {
        return Servings.getByDateAndFood(date, food);
    }

    @Override
    public void onCalculateStreakComplete(boolean success) {
        initFoodStreak(getServings());
    }

    public interface ClickListener {
        void onServingsChanged();
    }
}
