import { useEffect, useState } from "react";
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from "@mui/material";

import { Category } from "../../model/category.model";

interface Props {
  open: boolean;
  category: Category | undefined;
  onConfirm: (category: Category) => void;
  onCancel: () => void;
}

export default function EditCategory(props: Props) {
  const handleCancel = () => {
    props.onCancel();
  };

  function handleConfirm() {
    props.onConfirm(category);
  };

  const [category, setCategory] = useState<Category>({ name: "" });
  useEffect(() => { setCategory(props.category || { name: "", description: "", colour: "" }) }, [ props ]);

  function validateForm(): boolean {
    return category.name.trim().length > 0;
  }

  return (
    <Dialog open={ props.open } onClose={ handleCancel } fullWidth>
      <DialogTitle>{ props.category ? "Edit Category" : "Add Category" }</DialogTitle>
      <DialogContent>
        <TextField
          id="name" label="Category Name" autoFocus required
          margin="normal" fullWidth variant="standard"
          value={category.name}
          onChange={(e) => setCategory({ ...category, name: e.target.value })}
        />

        <TextField
          id="name" label="Description"
          margin="normal" fullWidth variant="standard"
          value={category.description}
          onChange={(e) => setCategory({ ...category, description: e.target.value })}
        />

        <TextField
          id="name" label="Colour"
          margin="normal" fullWidth variant="standard"
          value={category.colour}
          onChange={(e) => setCategory({ ...category, colour: e.target.value })}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={handleCancel} variant="outlined">Cancel</Button>
        <Button onClick={handleConfirm} variant="contained" disabled={!validateForm()}>
          { props.category ? "Save" : "Add" }
        </Button>
      </DialogActions>
    </Dialog>
  );
}
