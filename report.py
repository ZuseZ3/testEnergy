import matplotlib.pyplot as plt
import csv
import pandas as pd

from mpl_toolkits.axes_grid1 import make_axes_locatable
from mpl_toolkits.axes_grid1.axes_divider import make_axes_area_auto_adjustable

df = pd.read_csv("joularJX-methods-energy-filtered.csv")
sorted_df = df.sort_values(df.columns[0], ascending=False)
sorted_df.to_csv('joularJX-methods-energy-filtered.csv', index=False)

x = []
y = []

with open('joularJX-methods-energy-filtered.csv','r') as csvfile:
    plots = csv.reader(csvfile, delimiter = ',')

    for row in plots:
        x.append(row[0])
        y.append(float(row[1]))

fig, axes = plt.subplots(figsize=(100, 100))
make_axes_area_auto_adjustable(axes)

axes.tick_params(axis='y', labelsize=8)

plt.barh(x, y, color ='g', label="Jouls", height=0.5)
plt.ylabel('Methodname Date and Time')
plt.xlabel('Jouls')
plt.title('Energy consumption')
plt.legend()
plt.show()



