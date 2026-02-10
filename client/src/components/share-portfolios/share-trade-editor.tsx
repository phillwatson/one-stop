import { useEffect, useState, useCallback } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";

import { useMessageDispatch } from '../../contexts/messages/context';
import PortfolioService from '../../services/portfolio.service';
import { PortfolioResponse, ShareTrade, ShareTradeSummary } from "../../model/share-portfolio.model";
import { ShareIndex } from "../../model/share-indices.model";
import ShareTradeSummaryList from "./share-trade-summary";
import AddShareTradeDialog from "./add-share-trade";
import ConfirmationDialog from "../dialogs/confirm-dialog";

interface Props {
  portfolio?: PortfolioResponse;
}

export default function ShareTradeEditor(props: Props) {
  const showMessage = useMessageDispatch();
  const [holdings, setHoldings] = useState<Array<ShareTradeSummary>>([])
  const [selectedTrade, setSelectedTrade] = useState<ShareTrade | undefined>();
  const [shareTradeDialogOpen, setShareTradeDialogOpen] = useState<boolean>(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState<boolean>(false);
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

  function handleOpenAddTrade(shareIndexId?: string) {
    handleOpenAmendTrade({
      id: undefined,
      shareIndexId: shareIndexId,
      dateExecuted: new Date(),
      quantity: 0,
      pricePerShare: 0,
      totalCost: 0
    } as ShareTrade);
  }

  function handleOpenAmendTrade(trade: ShareTrade) {
    setSelectedTrade(trade);
    setShareTradeDialogOpen(true);
  }

  function handleCloseTradeDialog() {
    setShareTradeDialogOpen(false);
    setSelectedTrade(undefined);
  }

  function handleDeleteTrade(trade: ShareTrade) {
    setSelectedTrade(trade);
    setDeleteDialogOpen(true);
  }

  function onDeleteConfirmed() {
    PortfolioService.deleteShareTrade(selectedTrade!.id!)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Trade record deleted successfully` });
        refreshHoldings();
      })
      .catch(err => showMessage(err))
      .finally(() => setDeleteDialogOpen(false));
    setSelectedTrade(undefined);
  }

  function onAddOrAmendConfirmed(tradeId: string | undefined,
                                 shareIndex: ShareIndex,
                                 dateExecuted: Date,
                                 quantity: number,
                                 price: number) {
    setCreating(true);

    if (tradeId) {
      PortfolioService.updateShareTrade(tradeId, shareIndex.id, dateExecuted, quantity, price)
        .then(() => {
          showMessage({ type: 'add', level: 'success', text: `Trade for "${shareIndex.name}" amended successfully` });
          handleCloseTradeDialog();
          refreshHoldings();
        })
        .catch(err => showMessage(err))
        .finally(() => setCreating(false));
    } else {
      PortfolioService.recordShareTrade(props.portfolio!.id, shareIndex.id, dateExecuted, quantity, price)
        .then(() => {
          showMessage({ type: 'add', level: 'success', text: `Trade for "${shareIndex.name}" created successfully` });
          handleCloseTradeDialog();
          refreshHoldings();
        })
        .catch(err => showMessage(err))
        .finally(() => setCreating(false));
    }
  }

  return (
    <>
      <Box>
        <ShareTradeSummaryList holdings={ holdings }
          onAddTrade={ (holding) => handleOpenAddTrade(holding.shareIndexId) }
          onDeleteTrade={ handleDeleteTrade }
          onEditTrade={ handleOpenAmendTrade }/>
      </Box>

      <Box sx={{ mb: 2 }} display="flex" justifyContent="end">
        <Button variant="contained" onClick={ () => handleOpenAddTrade(undefined) } sx={{ mt: 2 }} >
          Add New Trade
        </Button>
      </Box>

      <AddShareTradeDialog
        open={shareTradeDialogOpen}
        shareTrade={ selectedTrade }
        onCancel={handleCloseTradeDialog}
        onConfirm={onAddOrAmendConfirmed}
        isCreating={creating}
      />

      <ConfirmationDialog open={deleteDialogOpen}
        title={"Delete Trade"}
        content={ [
            "Are you sure you want to delete this trade record?",
            "This action cannot be undone." ] }
        onConfirm={ onDeleteConfirmed }
        onCancel={() => setDeleteDialogOpen(false)}
      />

    </>
  );
}
