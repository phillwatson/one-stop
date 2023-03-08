import { useState } from "react";
import Alert, { AlertColor } from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";

interface ErrorMessage {
  severity: AlertColor | undefined,
  message: String | undefined
}

const [errors, setErrors] = useState<Array<ErrorMessage>>([]);

export function useErrorCards(error: ErrorMessage) {
  setErrors([ ...errors, error]);
}

export default function withErrorCards(WrappedComponent: any) {
  return function ErrorCards({ ...props }) {
    return (
      <>
        {
          errors.map( (e, index) => 
            <Snackbar key={index} open={errors.length > 0} autoHideDuration={6000} anchorOrigin={{ vertical: 'top', horizontal: 'right'}}>
              <Alert severity={e.severity} sx={{ width: '100%' }} onClose={() => {}}>{ e.message }</Alert> 
            </Snackbar>
          )
        }
        <WrappedComponent { ...props } />
      </>
    );
  };
}
