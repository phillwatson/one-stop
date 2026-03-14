import { useState } from "react";
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';

import ShareService from "../../services/share.service";
import { ShareIndex } from "../../model/share-indices.model";
import { useMessageDispatch } from "../../contexts/messages/context";

export default function ShareIndexRegistration() {
  const showMessage = useMessageDispatch();

  const [isin, setIsin] = useState<string>("");
  const [result, setResult] = useState<ShareIndex | undefined>(undefined);
  const [error, setError] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(false);

  function handleSubmit(event: any) {
    event.preventDefault();
    setResult(undefined);
    setError(undefined);

    if (!isin || isin.trim().length === 0) {
      setError("ISIN must be provided");
      return;
    }

    setLoading(true);
    ShareService.registerShareIndex(isin.trim())
      .then(response => {
        setResult(response);
      })
      .catch(err => {
        showMessage(err);
      })
      .finally(() => setLoading(false));
  }

  return (
    <form onSubmit={handleSubmit} className="panel">
      <TextField
        className="field"
        id="isin"
        label="ISIN"
        required
        variant="outlined"
        fullWidth
        margin="normal"
        value={isin}
        onChange={e => setIsin(e.target.value)}
      />

      <div className="panel">
        <Button type="submit" variant="contained" disabled={loading || isin.trim().length === 0}>
          {loading ? 'Registering...' : 'Register'}
        </Button>
      </div>

      {result && (
        <div className="panel">
          <strong>Registered share index:</strong>
          <pre>{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}

      {error && (
        <div className="panel" style={{ color: 'red' }}>
          {error}
        </div>
      )}
    </form>
  );
}
