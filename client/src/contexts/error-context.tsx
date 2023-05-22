import { createContext, forwardRef, useCallback, useContext, useEffect, useReducer } from "react";
import Snackbar from "@mui/material/Snackbar";
import Collapse from '@mui/material/Collapse';
import MuiAlert, { AlertProps } from '@mui/material/Alert';
import { TransitionGroup } from 'react-transition-group';

/**
 * See: https://react.dev/learn/scaling-up-with-reducer-and-context
 */


/**
 * Identifies an error's level of severity.
 */
type ErrorLevel = 'error' | 'warning' | 'info' | 'success';

/**
 * The record of an error that is to be reported.
 */
type ErrorMessage = {
  id: number;
  message: string;
  level: ErrorLevel;
}

/**
 * An action to be performed on the list of ErrorMessages.
 */
type ErrorsAction = 
  | { type: 'add', level: ErrorLevel, message: string }
  | { type: 'delete', id: number };


/**
 * Accepts actions and updates the list of ErrorMessages accordingly.
 * 
 * @param errors the list of ErrorMessages to be updated (the state)
 * @param action the action to be performed on the ErrorMessages.
 * @returns the modified copy of the given ErrorMessages.
 */
function errorsReducer(errors: ErrorMessage[], action: ErrorsAction): ErrorMessage[] {
  switch (action.type) {
    case 'add': {
      return [ { id: Date.now(), message: action.message, level: action.level }, ...errors];
    }

    case 'delete': {
      return errors.filter(e => e.id !== action.id);
    }

    default: {
      throw Error('Unknown errors action: ' + action);
    }
  }
}

/**
 * A context that allows components to access the list of ErrorMessages, and respond
 * to changes to the list.
 */
const ErrorsContext = createContext(Array<ErrorMessage>());
export function useErrors() {
  return useContext(ErrorsContext);
}

/**
 * A context that allows components to dispatch ErrorsAction to update the list of
 * ErrorMessages.
 */
const ErrorsDispatchContext = createContext(function(action: ErrorsAction) {});
export function useErrorsDispatch() {
  return useContext(ErrorsDispatchContext);
}

/**
 * A component that wraps child components within the context of a list of ErrorMessages.
 * 
 * @param props the child components to be wrapped.
 * @returns the ErrorsProvider component.
 */
export default function ErrorsProvider(props: React.PropsWithChildren) {
  const [errors, dispatch] = useReducer(errorsReducer, []);

  return (
    <ErrorsContext.Provider value={errors}>
      <ErrorsDispatchContext.Provider value={dispatch}>
        { props.children }

        <TransitionGroup>
        { errors.map((error, index) =>
          <Collapse key={error.id}>
            <ErrorToast error={error} dispatch={dispatch} index={index}/>
          </Collapse>
        )}
        </TransitionGroup>
      </ErrorsDispatchContext.Provider>
    </ErrorsContext.Provider>
  );
}

const Alert = forwardRef<HTMLDivElement, AlertProps>(function Alert(props, ref) {
  return <MuiAlert elevation={24} ref={ref} variant="filled" {...props} />;
});

interface ToastProps {
    error: ErrorMessage;
    dispatch: React.Dispatch<ErrorsAction>;
    index: number;
}

function ErrorToast(props: ToastProps) {
  const error = props.error;
  const dispatch = props.dispatch;

  const handleCloseAlert = useCallback(() => {
    dispatch({ type: 'delete', id: error.id });
  }, [error, dispatch]);

  useEffect(() => {
    // set a timeout for "success" messages
    const t = (error.level === 'success') ? setTimeout(() => { handleCloseAlert(); }, 5000) : null;
    return () => { if (t !== null) clearTimeout(t); }
  }, [error, handleCloseAlert]);

  const top = (props.index * 60) + "px";
  return (
    <Snackbar key={props.error.id} anchorOrigin={{ vertical: 'top', horizontal: 'right' }} sx={{ marginTop: top }} open={true}>
      <Alert onClose={handleCloseAlert} severity={props.error.level}>
        {props.error.message}
      </Alert>
    </Snackbar>
  );
}