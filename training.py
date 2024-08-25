import numpy as np
import tensorflow as tf
import pandas as pd
from sklearn.model_selection import KFold
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense

print("Abrindo dataset")
df = pd.read_csv("normalized_datasetAlbertoErick.csv").sample(n=100000)

data_array = np.array(df, dtype=float)
print(data_array[0])

features = np.array([data_array[0][0], data_array[0][1],data_array[0][2],data_array[0][3],data_array[0][4],data_array[0][5],data_array[0][6],data_array[0][7], data_array[0][10], data_array[0][11]])
labels = np.array([data_array[0][8], data_array[0][9]])

print("transformando dataset em features e labels")
for data in data_array:
    features = np.vstack((features, np.array([data[0], data[1],data[2],data[3],data[4],data[5],data[6],data[7], data[10], data[11]])))
    labels = np.vstack((labels, np.array([data[8], data[9]])))

features = np.delete(features, 0, 0)
labels = np.delete(labels, 0, 0)

print("Verificando se tem o mesmo tamanho")
print(np.size(features, 0))
print(np.size(labels, 0))

# Definindo 5 folds
kfold = KFold(n_splits=5, shuffle=True)

fold_no = 1
for train, test in kfold.split(features, labels):
    model = Sequential()
    model.add(Dense(64, input_dim=10, activation='relu'))
    model.add(Dense(32, activation='relu'))
    model.add(Dense(2, activation='sigmoid'))

    model.compile(loss='mean_absolute_error', optimizer='adam', metrics=['mean_absolute_error'])

    print('------------------------------------------------------------------------')
    print(f'Training for fold {fold_no} ...')

    history = model.fit(features[train], labels[train], epochs=10, batch_size=10)

    scores = model.evaluate(features[test], labels[test], verbose=0)
    print(f'Score for fold {fold_no}: {model.metrics_names[0]} of {scores[0]}; {model.metrics_names[1]} of {scores[1]*100}%')
    #acc_per_fold.append(scores[1] * 100)
    #loss_per_fold.append(scores[0])

    # Increase fold number
    fold_no = fold_no + 1

new_data = np.array([[0.31743341, 0.58566821, 0.88848922, 0.91628991, 0.1, 0.4375, 0.0, 0.59706121, 0.98446262, 0.53060559]])
prediction = model.predict(new_data)
print(prediction)
