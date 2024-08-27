import numpy as np
import tensorflow as tf
import pandas as pd
from sklearn.model_selection import KFold
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping

# Load the dataset
print("Opening dataset")
df = pd.read_csv("normalized_datasetAlbertoErick.csv")

print(df.size)

# Convert the dataset to a numpy array
data_array = np.array(df, dtype=float)

# Transform the dataset into features and labels arrays
print("Transforming dataset into features and labels")
labels = data_array[:, 7:9]
features = np.delete(data_array, [7, 8], axis=1)

# Define 5-fold cross-validation
kfold = KFold(n_splits=5, shuffle=True)

# Initialize fold counter
fold_no = 1

# Training loop for cross-validation
for train, test in kfold.split(features, labels):
    # Build the Sequential model
    model = Sequential()
    
    # Input layer is implicit in the first Dense layer
    # Hidden Layer 1: 128 neurons, ReLU activation
    model.add(Dense(128, input_dim=10, activation='relu'))
    
    # Hidden Layer 2: 64 neurons, ReLU activation
    model.add(Dense(64, activation='relu'))
    
    # Hidden Layer 3: 32 neurons, ReLU activation
    model.add(Dense(32, activation='relu'))
    
    # Output Layer: 2 neurons (x and y positions), Linear activation for regression
    model.add(Dense(2, activation='linear'))
    
    # Compile the model
    # Optimizer: Adam for efficient and adaptive learning
    # Loss function: MSE for penalizing large errors in regression
    # Metric: RMSE for evaluating model performance in the same units as the target variable
    model.compile(optimizer=Adam(), loss='mean_squared_error', metrics=[tf.keras.metrics.RootMeanSquaredError()])
    
    # Early Stopping Callback
    # Stops training when validation loss does not improve for 5 consecutive epochs
    early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)
    
    print('------------------------------------------------------------------------')
    print(f'Training for fold {fold_no} ...')
    
    # Train the model with early stopping
    history = model.fit(
        features[train], labels[train], 
        validation_data=(features[test], labels[test]),
        epochs=100,  # Start with a high number of epochs, but early stopping will prevent overfitting
        batch_size=10,
        callbacks=[early_stopping],  # Apply early stopping
        verbose=1  # Display training progress
    )
    
    # Evaluate the model's performance on the test data
    # Uses RMSE as the metric to assess prediction accuracy in the same units as the target variable
    scores = model.evaluate(features[test], labels[test], verbose=0)
    print(f'Score for fold {fold_no}: {model.metrics_names[0]} of {scores[0]}; {model.metrics_names[1]} of {scores[1]}')
    
    # Increase fold number
    fold_no += 1

# Make a prediction with new data (example)
new_data = np.array([[0.31743341, 0.58566821, 0.88848922, 0.91628991, 0.1, 0.4375, 0.0, 0.59706121, 0.98446262, 0.53060559]])
prediction = model.predict(new_data)
print("Predicted final enemy positions (x, y):", prediction)

# Save model weights to CSV files
print("Saving model weights to CSV files")
for i, layer in enumerate(model.layers):
    weights, biases = layer.get_weights()
    np.savetxt(f"layer_{i}_weights.csv", weights, delimiter=",")
    np.savetxt(f"layer_{i}_biases.csv", biases, delimiter=",")
