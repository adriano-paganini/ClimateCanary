import React from 'react';

interface NoDataOverlayProps {
    message?: string;
    height?: string;
}

const NoDataOverlay: React.FC<NoDataOverlayProps> = ({
    message = 'Keine Daten verfügbar',
    height = '220px',
}) => (
    <div style={{
        height,
        display:         'flex',
        alignItems:      'center',
        justifyContent:  'center',
        color:           '#9ca3af',
        backgroundColor: '#f3f4f6',
        borderRadius:    '6px',
        fontSize:        '0.9rem',
        gap:             '0.5rem',
    }}>
        <i className="pi pi-ban" style={{ fontSize: '1rem' }} />
        <span>{message}</span>
    </div>
);

export default NoDataOverlay;
