import pandas as pd
from sklearn.preprocessing import MinMaxScaler

# Load the CSV file into a DataFrame
df = pd.read_csv('datasetAlbertoErick.csv')

# Randomize the order of the rows
df = df.sample(frac=1).reset_index(drop=True)

# Initialize the scaler
scaler = MinMaxScaler()

# Normalize the DataFrame
normalized_df = pd.DataFrame(scaler.fit_transform(df), columns=df.columns)

# Verifica se há valores fora do intervalo [0, 1]
out_of_bounds = normalized_df[(normalized_df < 0) | (normalized_df > 1)].any()

if out_of_bounds.any():
    print("Atenção: As seguintes colunas têm valores fora do intervalo [0, 1]:")
    print(out_of_bounds[out_of_bounds].index.tolist())
else:
    print("Todos os valores estão no intervalo [0, 1].")

# Save to a new CSV file
normalized_df.to_csv('normalized_datasetAlbertoErick.csv', index=False)

# Imprime o número de linhas antes e depois da normalização (deve ser igual)
print(df.shape[0])
print(normalized_df.shape[0])
