import { ChangeEvent, useEffect, useState } from "react";

import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import TextField from "@mui/material/TextField";
import DialogActions from "@mui/material/DialogActions";
import Stack from '@mui/material/Stack';
import { FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from "@mui/material";

import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import 'dayjs/locale/de';
import 'dayjs/locale/en-gb';
import 'dayjs/locale/zh-cn';
import dayjs, { Dayjs } from 'dayjs';
import { defaultLocale } from '../../util/date-util';
import utc from 'dayjs/plugin/utc';

import { useMessageDispatch } from '../../contexts/messages/context';
import ShareService from '../../services/share.service';
import { ShareIndex } from "../../model/share-indices.model";
import { ShareTrade } from "../../model/share-portfolio.model";

dayjs.extend(utc);

const FLOAT_REGEX = /^-?\d*(\.\d+)?$/;

interface TradeDialogProps {
  open: boolean;
  shareTrade?: ShareTrade;
  onCancel: () => void;
  onConfirm: (tradeId: string | undefined, shareIndex: ShareIndex, dateExecuted: Date, quantity: number, price: number) => void;
  isCreating: boolean;
}

export default function AddShareTradeDialog(props: TradeDialogProps) {
  const showMessage = useMessageDispatch();

  const [shareIndices, setShareIndices] = useState<ShareIndex[]>([]);
  const [shareIndex, setShareIndex] = useState<ShareIndex | undefined>(undefined);
  const [dateExecuted, setDateExecuted] = useState<Dayjs>(dayjs());
  const [quantity, setQuantity] = useState<string>('');
  const [price, setPrice] = useState<string>('');

  useEffect(() => {
    ShareService.fetchAllIndices()
      .then(indices => { setShareIndices(indices); return indices; })
      .catch(err => showMessage(err));
  }, [ showMessage ]);

  useEffect(() => {
    if (props.shareTrade) {
      if (props.shareTrade.shareIndexId !== undefined) {
        setShareIndex(shareIndices.find(index => index.id === props.shareTrade?.shareIndexId))
      } else {
        setShareIndex(undefined);
      }

      setDateExecuted(dayjs(props.shareTrade.dateExecuted));
      setQuantity(props.shareTrade.quantity.toString())
      setPrice(props.shareTrade.pricePerShare.toString());

    }
    }, [ props.shareTrade, shareIndices ]);

  function selectIndex(event: SelectChangeEvent) {
    setShareIndex(shareIndices.find(s => s.id === event.target.value));
  }

  function enterQuantity(event: ChangeEvent) {
    const value: string = (event.target as HTMLInputElement).value;
    setQuantity(value);
  }

  function enterPrice(event: ChangeEvent) {
    const value: string = (event.target as HTMLInputElement).value;
    setPrice(value);
  }

  function validateForm(): boolean {
    const p = price?.trim() || '';
    const validPrice = (p.length > 0) && (p.match(FLOAT_REGEX) !== null);

    const q = quantity?.trim() || '';
    const validQuantity = (q.length > 0) && (q.match(FLOAT_REGEX) !== null);

    return shareIndex !== undefined
      && dateExecuted !== undefined
      && (validPrice && parseFloat(price) > 0)
      && (validQuantity && parseFloat(quantity) !== 0);
  }

  function handleConfirm() {
    props.onConfirm(props.shareTrade?.id, shareIndex!, dateExecuted.toDate(), parseFloat(quantity), parseFloat(price));
  }

  return (
    <Dialog open={props.open} onClose={props.onCancel} fullWidth>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white' }}>
        { (props.shareTrade?.id ? 'Amend Trade' : 'Record New Trade') }
      </DialogTitle>
      <DialogContent>
          <Stack spacing={3} paddingTop={3} direction="column">
            { props?.shareTrade?.shareIndexId !== undefined &&
              <TextField
                id="share-index"
                label="Share Index"
                variant="standard"
                fullWidth
                value={shareIndex?.name}
                disabled
              />
            }
            { props?.shareTrade?.shareIndexId === undefined &&
              <FormControl fullWidth margin="normal" sx={{ marginBottom: 3 }}>
                <InputLabel id="select-index-label">Share Index</InputLabel>
                <Select id="select-index" labelId="select-index-label" label="Share Index"
                  fullWidth required
                  onChange={ selectIndex }
                  value={ shareIndex?.id || '' }>
                  {
                    shareIndices.map(index => <MenuItem key={ index.id } value={ index.id }>{ index.name }</MenuItem> )
                  }
                </Select>
              </FormControl>
            }

            <Stack spacing={2} direction="row">
              <LocalizationProvider dateAdapter={ AdapterDayjs } adapterLocale={ defaultLocale.toLowerCase() }>
                <DatePicker disableFuture label="Execution Date"
                  value={ dateExecuted }
                  onChange={ (value: Dayjs | null) => { setDateExecuted(value || dayjs()) }}/>
              </LocalizationProvider>

              <TextField
                id="quantity"
                label="Quantity"
                variant="standard"
                required
                value={quantity}
                onChange={ enterQuantity }
              />

              <TextField
                id="price"
                label="Purchase Price"
                variant="standard"
                required
                value={price}
                onChange={ enterPrice }
              />
            </Stack>
          </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={props.onCancel} variant="outlined">Cancel</Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          disabled={!validateForm() || props.isCreating}
        >
          { (props.shareTrade?.id ? 'Update' : 'Create') }
        </Button>
      </DialogActions>
    </Dialog>
  );
}
