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
