import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np

# --- Daten ---
times = ["08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"]
x = np.arange(len(times))

data = {
    "temp":       [19.2, 20.1, 21.4, 22.8, 23.5, 24.1, 24.9, 23.8, 22.3, 21.1],
    "humidity":   [42,   44,   46,   48,   51,   53,   55,   54,   51,   49  ],
    "airQuality": [620,  680,  750,  890,  1050, 1180, 1320, 1150, 920,  740 ],
}

thresholds = {
    "temp":       {"upper": 24.0, "lower": 18.0,  "unit": "°C",      "label": "Temperatur"},
    "humidity":   {"upper": 60.0, "lower": 40.0,  "unit": "%",       "label": "Luftfeuchtigkeit"},
    "airQuality": {"upper": 1000, "lower": None,   "unit": "ppm CO₂", "label": "Luftqualität"},
}

colors = {
    "temp":       "#e05c3a",
    "humidity":   "#3a8be0",
    "airQuality": "#2db37a",
}

# --- Plot ---
fig, axes = plt.subplots(3, 1, figsize=(12, 10), facecolor="#0f0f1a")
fig.suptitle("ClimateCanary — Raum 3.14 — Verlaufsansicht", 
             color="white", fontsize=14, fontweight="bold", y=0.98)

for ax, (metric, values) in zip(axes, data.items()):
    t = thresholds[metric]
    c = colors[metric]

    ax.set_facecolor("#13131f")
    for spine in ax.spines.values():
        spine.set_edgecolor("#2a2a3a")

    # Werte über Grenzwert rot markieren
    violated_x, violated_y = [], []
    for i, v in enumerate(values):
        if (t["upper"] and v > t["upper"]) or (t["lower"] and v < t["lower"]):
            violated_x.append(x[i])
            violated_y.append(v)

    # Grau hinterlegter Bereich zwischen Grenzwerten
    if t["lower"]:
        ax.axhspan(t["lower"], t["upper"], alpha=0.04, color=c)

    # Oberer Grenzwert
    ax.axhline(y=t["upper"], color="#e05c3a", linestyle="--", linewidth=1.4, alpha=0.8)
    ax.text(x[-1] + 0.05, t["upper"], f'Max {t["upper"]} {t["unit"]}',
            color="#e05c3a", fontsize=8, va="center")

    # Unterer Grenzwert
    if t["lower"]:
        ax.axhline(y=t["lower"], color="#f0a500", linestyle="--", linewidth=1.4, alpha=0.8)
        ax.text(x[-1] + 0.05, t["lower"], f'Min {t["lower"]} {t["unit"]}',
                color="#f0a500", fontsize=8, va="center")

    # Linie
    ax.plot(x, values, color=c, linewidth=2.2, zorder=3)

    # Normale Punkte
    ax.scatter(x, values, color=c, s=40, zorder=4)

    # Verletzte Punkte
    if violated_x:
        ax.scatter(violated_x, violated_y, color="#e05c3a", s=90,
                   zorder=5, edgecolors="white", linewidths=1.2)

    # Achsen
    ax.set_xticks(x)
    ax.set_xticklabels(times, color="#666", fontsize=9)
    ax.tick_params(axis="y", colors="#666", labelsize=9)
    ax.tick_params(axis="x", length=0)
    ax.grid(axis="y", color="#1e1e30", linewidth=0.8)
    ax.set_xlim(-0.3, len(times) - 0.3)

    # Ylabel
    ax.set_ylabel(f'{t["label"]} ({t["unit"]})', color="#aaa", fontsize=9)

    # Legende
    patches = [
        mpatches.Patch(color=c,        label="Messwerte"),
        mpatches.Patch(color="#e05c3a", label="Oberer Grenzwert"),
    ]
    if t["lower"]:
        patches.append(mpatches.Patch(color="#f0a500", label="Unterer Grenzwert"))
    if violated_x:
        patches.append(mpatches.Patch(color="#e05c3a", label="⚠ Grenzwert überschritten"))
    ax.legend(handles=patches, loc="upper left", fontsize=8,
              facecolor="#1a1a2e", edgecolor="#333", labelcolor="white")

plt.tight_layout(rect=[0, 0, 0.93, 0.97])
plt.savefig("verlaufsansicht_mockup.png", dpi=150, bbox_inches="tight",
            facecolor="#0f0f1a")
plt.show()
print("Gespeichert als verlaufsansicht_mockup.png")