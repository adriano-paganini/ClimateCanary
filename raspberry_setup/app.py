from fastapi import FastAPI

app = FastAPI()

@app.get("/")
async def read_root():
    return {"message": "Raspberry Pi Gateway (FastAPI)", "status": "online"}

@app.get("/test")
async def read_test():
    return {
        "device": "Raspberry Pi",
        "tech": "FastAPI",
        "ble_ready": True
    }

# --- CLIENT-LOGIK ---

SPRING_BOOT_URL = "http://10.33.23.18:8080/api/status"


@app.get("/data-from-pi")
async def give_string_to_backend():
    return "Hallo Backend, hier spricht dein Raspberry Pi!"

@app.get("/trigger-backend-call")
async def trigger_external_call():
    try:
        payload = {"message": "Aktueller Status vom Pi: Betriebsbereit"}
        response = requests.post(SPRING_BOOT_URL, json=payload, timeout=5)
        return {"status": "erfolgreich", "backend_antwort": response.text}
    except Exception as e:
        return {"status": "fehler", "error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)