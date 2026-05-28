FROM python:3.13-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    bluez \
    dbus \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV PYTHONUNBUFFERED=1
ENV TZ=Europe/Vienna

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY *.py conf.yml ./

EXPOSE 8000

CMD ["python", "main.py"]
