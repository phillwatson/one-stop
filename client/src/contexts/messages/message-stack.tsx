import "./message-stack.css";
import { Message, MessageAction } from "./model";
import MessagePane from './message-pane';

interface MessageStackProps {
  messages: Array<Message>;
  dispatch: React.Dispatch<MessageAction>;
}

export default function MessageStack(props: MessageStackProps) {
  return (
    <div className="message-stack" >
      { props.messages.map((message) =>
        <MessagePane key={ message.id } message={ message } dispatch={ props.dispatch }/>
      )}
    </div>
  );
}
