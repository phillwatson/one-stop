import { useEffect, useState, useCallback } from "react";
import dayjs, { Dayjs } from 'dayjs';
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";

import { useMessageDispatch } from '../../contexts/messages/context';
import PortfolioService from '../../services/portfolio.service';
import ShareService from '../../services/share.service';
import { PortfolioResponse, ShareTrade, ShareHoldingSummary } from "../../model/share-portfolio.model";
import { HistoricalPriceResponse, ShareIndex } from "../../model/share-indices.model";
import HoldingsSummaryList from "./holdings-summary-list";
import AddShareTradeDialog from "./add-share-trade";
import ConfirmationDialog from "../dialogs/confirm-dialog";
import HoldingsGraph from "./holdings-graph";
import SimpleDateRange from "../graph/simple-date-range";

interface Props {
  portfolio?: PortfolioResponse;
}

export interface HoldingPrices {
  holding: ShareHoldingSummary; 
  prices?: Array<HistoricalPriceResponse>;
}

export default function HoldingsEditor(props: Props) {
  const showMessage = useMessageDispatch();

  const [holdings, setHoldings] = useState<Array<ShareHoldingSummary>>([])
  const [trades, setTrades] = useState<Map<string,ShareTrade[]>>(new Map());
  const [dateRange, setDateRange] = useState<Dayjs[]>([ dayjs().subtract(365, 'days'), dayjs() ]);
  const [holdingPrices, setHoldingPrices] = useState<Array<HoldingPrices>>([]);

  const [selectedHolding, setSelectedHolding] = useState<ShareHoldingSummary | undefined>();
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
    // fetch the trades for each holding
    const requests = holdings
      .map(holding => PortfolioService.fetchShareTrades(holding.portfolioId, holding.shareIndexId));

    // wait for requests to complete
    Promise.all(requests)
      // convert the results to a Map, indexed by the share-index-id
      .then(allTrades => allTrades.reduce((result, trades) =>
        (trades.length > 0) ? result.set(trades[0].shareIndexId, trades) : result
        , new Map<string,ShareTrade[]>())
      )
      .then(result => setTrades(result))
      .catch(err => showMessage(err));
  }, [ showMessage, holdings ])

  useEffect(() => {
    // fetch the historical prices each holding - filtered according to date range
    const requests = holdings.map(holding =>
      ShareService.getPrices(holding.shareIndexId, dateRange[0].toDate(), dateRange[1].toDate())
        .then(prices => (
          {
            holding: holding,
            prices: prices
          } as HoldingPrices
        ))
      );

    // wait for requests to complete
    Promise.all(requests)
      .then(prices => setHoldingPrices(prices))
      .catch(err => showMessage(err));
  }, [ showMessage, dateRange, holdings ])

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
      <Box paddingLeft={{ xs: 0, sm: '5px', md: '25px', lg: '80px', xl: '200px' }}
           paddingRight={{ xs: 0, sm: '5px', md: '25px', lg: '80px', xl: '200px' }} >

        { holdings.length > 0 &&
        <>
          <SimpleDateRange dateRange={ dateRange } onSelect={ setDateRange }/>
          <HoldingsGraph holdingPrices={ selectedHolding
              ? holdingPrices.filter(h => h.holding.shareIndexId === selectedHolding.shareIndexId)
              : holdingPrices }
              holdingTrades={ trades }
          />
        </>
        }
        <HoldingsSummaryList holdings={ holdings } selectedHolding={ selectedHolding } holdingTrades={ trades }
          onAddHolding={ (holding) => handleOpenAddTrade(holding.shareIndexId) }
          onDeleteTrade={ handleDeleteTrade }
          onEditTrade={ handleOpenAmendTrade }
          onSelectHolding={ setSelectedHolding } />
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
