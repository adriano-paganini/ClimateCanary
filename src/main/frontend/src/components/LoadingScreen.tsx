import {ProgressSpinner} from "primereact/progressspinner";

const LoadingScreen = () => (
    <div
        className="loading-screen"
        aria-label="Loading"
        style={{display: "flex", alignItems: "center", justifyContent: "center", minHeight: "100vh", width: "100%"}}
    >
        <ProgressSpinner/>
    </div>
);

export default LoadingScreen;
