import React, { useState } from "react";
import Snackbar from "@mui/material/Snackbar";
import Slide from "@mui/material/Slide";
import Alert, { AlertColor } from "@mui/material/Alert";

function withNotification(WrappedComponent: any) {
    const [open, setOpen] = useState(false);
    const [message, setMessage] = useState("I'm a custom snackbar");
    const [duration, setDuration] = useState(2000);
    const [severity, setSeverity] = useState('success' as AlertColor); /** error | warning | info */

    function handleClose() {
        setOpen(false);
    }

    return class extends React.Component {
        constructor(props: any) {
            super(props);
        }

        render(): any {
            <>
                <WrappedComponent {...this.props} />
                <Snackbar
                    anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
                    autoHideDuration={duration}
                    open={open}
                    onClose={handleClose}
                    TransitionComponent={Slide}
                    >
                    <Alert variant="filled" onClose={handleClose} severity={severity}>
                        {message}
                    </Alert>
                </Snackbar>
            </>
        }
    }
}