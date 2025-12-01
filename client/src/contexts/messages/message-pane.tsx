import { useCallback, useEffect } from "react";
import Alert from "@mui/material/Alert";
import Slide from "@mui/material/Slide";

import { Message, MessageAction } from "./model";

interface MessageProps {
  message: Message;
  dispatch: React.Dispatch<MessageAction>;
}

// those severities that should be auto-closed
const AUTO_CLOSE = ['success', 'info'];

export default function MessagePane(props: MessageProps) {
  const message = props.message;
  const dispatch = props.dispatch;

  const handleCloseAlert = useCallback(() => {
    dispatch({ type: 'delete', id: message.id });
  }, [message, dispatch]);

  useEffect(() => {
    const timer = (AUTO_CLOSE.includes(message.level)) ? setTimeout(() => { handleCloseAlert(); }, 5000) : null;
    return () => { if (timer !== null) clearTimeout(timer); }
  }, [message, handleCloseAlert]);

  return (
    <Slide direction="left" in={ true }>
      <Alert elevation={24} onClose={ handleCloseAlert } severity={ message.level }>
        {
          message.text.split('\n').map((line, index) => <div key={ index }>{ line }</div>)
        }
      </Alert>
    </Slide>
  );
}