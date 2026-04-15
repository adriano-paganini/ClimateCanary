import aiosqlite

BELOW_MIN_OCCUPANCY: bool = False

async def load_occupancy_from_db(db):
    global BELOW_MIN_OCCUPANCY
    async with db.execute("SELECT value FROM pi_state WHERE key='below_min_occupancy'") as cursor:
        row = await cursor.fetchone()
        if row:
            BELOW_MIN_OCCUPANCY = row[0] == "1"
            print(f"[STATE] restored below_min_occupancy={BELOW_MIN_OCCUPANCY}")