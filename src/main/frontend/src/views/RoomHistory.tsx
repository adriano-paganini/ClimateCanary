import { useParams, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import NavbarComponent from '../components/NavbarComponent';
import { ROUTES } from '../utilities/routes.paths';

const RoomHistory = () => {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate = useNavigate();

    return (
        <div>
            <NavbarComponent />
            <div className="m-4">
                <Button
                    icon="pi pi-arrow-left"
                    label="Back to Dashboard"
                    className="p-button-text mb-4"
                    onClick={() => navigate(ROUTES.DASHBOARD)}
                />
                <h2>Room History — Room {roomId}</h2>
                <p style={{ color: '#6b7280' }}>
                    History view is not yet implemented (issue #13).
                </p>
            </div>
        </div>
    );
};

export default RoomHistory;
