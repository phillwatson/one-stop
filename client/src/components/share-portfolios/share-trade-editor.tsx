import { useEffect, useState, useCallback } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";

import { useMessageDispatch } from '../../contexts/messages/context';
import PortfolioService from '../../services/portfolio.service';
import ShareTradeSummaryList from "./share-trade-summary";
import { PortfolioResponse, ShareTradeSummary } from "../../model/share-portfolio.model";
import { ShareIndex } from "../../model/share-indices.model";
import AddShareTradeDialog from "./add-share-trade";

interface Props {
  portfolio?: PortfolioResponse;
}

export default function ShareTradeEditor(props: Props) {
  const showMessage = useMessageDispatch();
  const [ holdings, setHoldings ] = useState<Array<ShareTradeSummary>>([])
  const [shareTradeDialogOpen, setShareTradeDialogOpen] = useState<boolean>(false);
  const [creating, setCreating] = useState<boolean>(false);

  const refreshHoldings = useCallback(() => {
    if (props.portfolio) {
      PortfolioService.getPortfolioHoldings(props.portfolio.id)
        .then(response => setHoldings(response))
        .catch(err => showMessage(err));
    } else {
      setHoldings([]);
    }
  }, [ showMessage, props.portfolio ])

  useEffect(() => {
    refreshHoldings();
  }, [ refreshHoldings ]);

  function handleOpenAddTradeDialog() {
    setShareTradeDialogOpen(true);
  }

  function handleCloseAddTradeDialog() {
    setShareTradeDialogOpen(false);
  }

  function handleAddTrade(shareIndex: ShareIndex, dateExecuted: Date, quantity: number, price: number) {
    setCreating(true);
    PortfolioService.recordShareTrade(props.portfolio!.id, shareIndex.id, dateExecuted, quantity, price)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Trade for "${shareIndex.name}" created successfully` });
        handleCloseAddTradeDialog();
        refreshHoldings();
      })
      .catch(err => showMessage(err))
      .finally(() => setCreating(false));
  }

  return (
    <>
      <Box>
        <ShareTradeSummaryList holdings={ holdings } />
      </Box>

      <Box sx={{ mb: 2 }} display="flex" justifyContent="end">
        <Button variant="contained" onClick={ handleOpenAddTradeDialog } sx={{ mt: 2 }} >
          Add Trade
        </Button>
      </Box>

      <AddShareTradeDialog
        open={shareTradeDialogOpen}
        onCancel={handleCloseAddTradeDialog}
        onConfirm={handleAddTrade}
        isCreating={creating}
      />
    </>
  );
}
