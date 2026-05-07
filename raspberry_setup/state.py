PRIVACY_MODE: bool = False


def set_privacy_mode(value: bool) -> None:
    global PRIVACY_MODE
    PRIVACY_MODE = value
    print(f"[STATE] privacy_mode={'ON' if value else 'OFF'}")


def get_privacy_mode() -> bool:
    return PRIVACY_MODE