/* eslint-disable no-magic-numbers */
/* eslint new-cap: [1, {capIsNewExceptions: ["DataTable"]}] */
/* global knimeTileView:false, $:false, KnimeBaseTableViewer:false */
window.generalPurposeLabelingWidget = (function () {
    var labelingWidget = {};
    var _rowKeys = {};
    var _rowKeysOnly = [];
    var _tileViewData = null;
    var _initialized = false;
    var _defaultHeaderColor = '#404040';
    var _skipHeaderColor = '#808080';
    var _masterColors = [2062516, 3383340, 14883356, 16744192, 6962586, 10931939, 11722634,
        2062516, 3383340, 14883356, 16744192, 6962586, 10931939, 11722634,
        2062516, 3383340, 14883356, 16744192, 6962586, 10931939, 11722634,
        2062516, 3383340, 14883356, 16744192, 6962586, 10931939, 11722634];
    var _representation,
        _value,
        _tileView,
        _drawbackTimeout,

        // Function definitions
        _createContainer, _updateButtonClasses, _getHexColor, _changeSkipButton, _setupSkipButtonHandler,
        _labelAndLoadNext, _setupAddClassesButtonHandler, _createClassEditorDialog, _updateDialogClasses,
        _initializeView, _bindArrowKeys, _updateLabelClasses, _updateDropdownClasses, _filterData,
        _getAllPossibleValues, _hexToRgb, _selectNextTile, _getColorValue, _createRemoveDialog,
        _combinePossibleValues, _checkIfIsCurrentlyDisplayed, _changeToDefaultHeaderColor,
        _initializeLabels, _getTotalNumberLabeled, _countTrueSelectedValues, _setSelectedTilesText;

    labelingWidget.init = function (representation, value) {
        _representation = representation;
        // Check if actually data is present
        if (!_representation.table) {
            var missingDataText = document.createElement('div');
            missingDataText.innerHTML = 'Error: No data available';
            document.body.appendChild(missingDataText);
            return;
        }
        // Ignore previous defined row color headers and reset to default color
        _representation.table.spec.rowColorValues = _changeToDefaultHeaderColor(
            _representation.table.spec.rowColorValues, _defaultHeaderColor
        );
        _representation.table.rows.forEach((row, rowInd) => {
            var label = typeof value.labels[row] === 'undefined' ? null : value.labels[row];
            _rowKeys[row.rowKey] = {
                rowInd,
                label,
                originalRowKey: rowInd
            };
            _rowKeysOnly.push(row.rowKey);
        });
        _value = value;
        _value.possiblevalues = _combinePossibleValues(_representation, _value);
        // TODO remove and add configurable color options
        _value.possiblevalues.forEach((labelValue, labelInd) => {
            _value.colors[labelValue] = _masterColors[labelInd];
        });
        _createContainer();
        _bindArrowKeys();
    };

    /**
     * Function to validate the correct initialization of the view
     * @returns {boolean} - returns true in order to tell that everything worked
     */
    labelingWidget.validate = function () {
        return true;
    };

    /**
     * Overwrites the getComponentValue function in order to properly map undefined values to '?' which will
     * be parsed to a missing value on the java side
     * @returns {Object} - returns the modified _value object
     */
    labelingWidget.getComponentValue = function () {
        window.knimeTileView.getComponentValue.apply(window.knimeTileView);
        // Map '?' to undefined to serialize to missing values
        _rowKeysOnly.forEach((row) => {
            if (typeof _value.labels[row] === 'undefined' || _value.labels[row] === '?') {
                _value.labels[row] = '?';
            }
        });
        return _value;
    };

    /**
     * Changes any existing colored header row to a default color
     * @param {Array} rowColorValues - an array with all rows which color should be set to the provided color
     * @param {int} colorValue - the default color of the tile header
     * @returns {Array} - returns the provided array with set color on all rows
     */
    _changeToDefaultHeaderColor = function (rowColorValues, colorValue) {
        rowColorValues.forEach((val, index) => {
            rowColorValues[index] = colorValue;
        });
        return rowColorValues;
    };

    /**
     * Combines possible values(labels) of the representation-object and the value-object
     * @param {Object} representation - the representation of the widget
     * @param {Object} value - the value of the widget
     * @returns {Array} - returns an array of all unique possible labels
     */
    _combinePossibleValues = function (representation, value) {
        var possibleValues = {};
        representation.possiblevalues.forEach((row) => {
            possibleValues[row] = true;
        });
        value.possiblevalues.forEach((row) => {
            possibleValues[row] = true;
        });
        return Object.keys(possibleValues);
    };

    /**
     * Initializes the view
     * @param {Object} representation - the representation of the widget
     * @param {Object} value - the value of the widget
     * @param {boolean} redraw - true if the view should be redrawn
     * @returns {undefined}
     */
    _initializeView = function (representation, value, redraw) {
        if (_initialized === false) {
            _initialized = true;
            if (Object.keys(value.labels).length > 0) {
                var colorMap = {
                    '?': _hexToRgb(_skipHeaderColor),
                    Skip: _hexToRgb(_skipHeaderColor)
                };

                value.possiblevalues.forEach((labelValue, labelInd) => {
                    var color = value.colors[labelValue] || representation.colors[labelInd];
                    colorMap[labelValue] = _hexToRgb(_getHexColor(color));
                });

                _initializeLabels(value.labels, colorMap, _rowKeys, redraw);
            }
        }
    };

    /**
     * Mostly copied from baseTableViewer except for two lines which set to the correct selection.
     * This is necessary, as the correct page is drawn after a timeout.
     * @returns {undefined}
     */
    window.knimeTileView._applyViewValueOld = window.knimeTileView._applyViewValue;
    window.knimeTileView._applyViewValue = function () {
        if (this._representation.enableSearching && this._value.filterString) {
            this._dataTable.search(this._value.filterString);
        }
        if (this._representation.enableColumnSearching && this._value.columnFilterStrings) {
            for (var i = 0; i < this._value.columnFilterStrings.length; i++) {
                var curValue = this._value.columnFilterStrings[i];
                if (curValue.length > 0) {
                    var column = this._dataTable.column(i);
                    $('input', column.footer()).val(curValue);
                    column.search(curValue);
                }
            }
        }
        if (this._representation.enablePaging && this._value.currentPage) {
            var self = this;
            setTimeout(() => {
                self._dataTable.page(self._value.currentPage).draw('page');
                _setSelectedTilesText(_countTrueSelectedValues(self._selection));
            }, 0);
        }
    };

    /**
     * Overwrite the selectionChanged function to react to selection events from other views.
     * @param {Object} data - the selection change Object provided by the tile view selectionChanged function
     * @returns {undefined}
     */
    window.knimeTileView._selectionChangedOld = window.knimeTileView._selectionChanged;
    window.knimeTileView._selectionChanged = function (data) {
        window.knimeTileView._selectionChangedOld.apply(this, [data]);
        if (data.elements) {
            for (var ele = 0; ele < data.elements[0].rows.length; ele++) {
                knimeTileView._selection[data.elements[0].rows[ele]] = true;
            }
        } else {
            Object.keys(knimeTileView._selection).forEach(row => {
                if (!knimeTileView._selection[row] &&
                        !knimeTileView._knimeTable.isRowIncludedInFilter(row, knimeTileView._currentFilter)) {
                    delete knimeTileView._selection[row];
                }
            });
            this._value.selection = this._selection;
        }
        _setSelectedTilesText(_countTrueSelectedValues(knimeTileView._selection));
        _changeSkipButton();
    };

    /**
     * Overwrite the filterChanged function to react to filter events from other views.
     * @param {Object} data - the filter change Object provided by the tile view filterChanged function
     * @returns {undefined}
     */
    window.knimeTileView._filterChangedOld = window.knimeTileView._filterChanged;
    window.knimeTileView._filterChanged = function (data) {
        window.knimeTileView._filterChangedOld.apply(this, [data]);
        var counter = 0;
        _rowKeysOnly = [];
        _representation.table.rows.forEach((row, rowInd) => {
            if (!knimeTileView._knimeTable.isRowIncludedInFilter(row.rowKey, knimeTileView._currentFilter)) {
                _tileView._selection[row.rowKey] = false;
                return;
            }
            var label = typeof _value.labels[row.rowKey] === 'undefined' ? null : _value.labels[row.rowKey];
            _rowKeys[row.rowKey] = {
                rowInd: counter,
                label,
                originalRowKey: rowInd
            };
            counter++;
            if (knimeService.isRowSelected(knimeTileView._representation.table.id, row.rowKey)) {
                _tileView._selection[row.rowKey] = true;
            }
            _rowKeysOnly.push(row.rowKey);
        });
        if (!data.reevaluate) {
            _initialized = false;
            _initializeView(knimeTileView._representation, knimeTileView._value, true);
        }
    };

    /**
     * Overwrite the dataTableDrawCallback function to load selection into view, if there was a previous selection
     * Add a timeout, as otherwise the method is called everytime a page is rendered.
     * @returns {undefined}
     */
    window.knimeTileView._dataTableDrawCallbackOld = window.knimeTileView._dataTableDrawCallback;
    window.knimeTileView._dataTableDrawCallback = function () {
        window.knimeTileView._dataTableDrawCallbackOld.apply(this);
        if (window.knimeTileView._dataTable !== null && !_initialized) {
            clearTimeout(_drawbackTimeout);
            _drawbackTimeout = setTimeout(() => {
                _initializeView(_representation, _value);
            }, 1000);
        }
    };

    /**
     * Applies the selection to the currently display page
     * @returns {undefined}
     */
    window.knimeTileView._setSelectionOnPageOld = window.knimeTileView._setSelectionOnPage;
    window.knimeTileView._setSelectionOnPage = function () {
        window.knimeTileView._setSelectionOnPageOld.apply(this);
        var curCheckboxes = this._dataTable.column(0, {
            page: 'current'
        }).nodes().to$().children();
        for (var i = 0; i < curCheckboxes.length; i++) {
            var checkbox = curCheckboxes[i];
            checkbox.checked = knimeTileView._selection[checkbox.value];
            // set tr style
            var $tr = $(checkbox).parent().parent();
            if (checkbox.checked) {
                $tr.addClass('knime-selected');
            } else {
                $tr.removeClass('knime-selected');
            }
            if ('indeterminate' in checkbox) {
                if (!checkbox.checked && this._partialSelectedRows.indexOf(checkbox.value) > -1) {
                    checkbox.indeterminate = true;
                } else {
                    checkbox.indeterminate = false;
                }
            }
            if (knimeTileView._value.labels[checkbox.value]) {
                checkbox.parentNode.parentNode.labeled = true;
            }
        }
    };

    /**
     * Overwrite the menu building function
     * @returns {undefined}
     */
    window.knimeTileView._buildMenuOld = window.knimeTileView._buildMenu;
    window.knimeTileView._buildMenu = function () {
        window.knimeTileView._buildMenuOld.apply(this);
        // Overwrite the floating calculation of the table view, as floating is wanted in the labeling view
        knimeService.floatingHeader(true);
        var self = this;
        if (!this._representation.showUnlabeledOnly) {
            var showUnlabeledCheckbox = knimeService.createMenuCheckbox('showUnlabeledCheckbox',
                this._value.showUnlabeledOnly, () => {
                    if (showUnlabeledCheckbox.checked) {
                        self._filterLabeledData();
                        self._value.showUnlabeledOnly = true;
                    } else {
                        self._resetFilter();
                        self._value.showUnlabeledOnly = false;
                    }
                });
            knimeService.addMenuItem('Show only unlabeled data', null, showUnlabeledCheckbox);
        }
        if (this._representation.labelcreation) {
            var addClassesButton = document.createElement('button');
            addClassesButton.type = 'button';
            addClassesButton.id = 'btnEditClasses';
            addClassesButton.innerHTML = '&#x1F527;';
            addClassesButton.onclick = function () {
                document.getElementById('dlgEdit').showModal();
            };
            knimeService.addMenuItem('Create custom classes', null, addClassesButton);
        }
        if (!this._representation.autoSelectNext) {
            var autoSelectCheckbox = knimeService.createMenuCheckbox('autoSelectCheckbox',
                this._value.autoSelectNextTile, () => {
                    if (autoSelectCheckbox.checked) {
                        self._value.autoSelectNextTile = true;
                    } else {
                        self._value.autoSelectNextTile = false;
                    }
                });
            knimeService.addMenuItem('Auto select next tile when labeled', null, autoSelectCheckbox);
        }
    };

    /**
     * Overwrite the click selection behavior
     * @returns {undefined}
     */
    window.knimeTileView._setSelectionHandlers = function () {
        KnimeBaseTableViewer.prototype._setSelectionHandlers.apply(this);
        var clearSelectionButton = $('#pagedTableClearSelectionButton');
        // Overwrite clearSelectionButton to properly deselect selected tiles
        clearSelectionButton.click(() => {
            _tileView._selectAll(false);
            knimeTileView._selection = [];
            _setSelectedTilesText(0);
            _changeSkipButton();
        });
        if (!this._representation.enableSelection) {
            return;
        }
        this._getJQueryTable().find('tbody').addClass('knime-selection-enabled').on('click', 'tr', (e) => {
            if (e.target && e.target.tagName === 'INPUT' && e.target.type === 'checkbox') {
                if (e.target.checked) {
                    knimeTileView._selection[e.target.value] = true;
                    _setSelectedTilesText(_countTrueSelectedValues(knimeTileView._selection));
                    _changeSkipButton();
                } else {
                    if (knimeTileView._selection[e.target.value]) {
                        delete knimeTileView._selection[e.target.value];
                        _setSelectedTilesText(_countTrueSelectedValues(knimeTileView._selection));
                    }
                    _changeSkipButton();
                }

            } else {
                $(e.currentTarget).find('input[type="checkbox"]').click();
            }
        });
    };

    /**
     * Initializes the tileView and Creates all HTML elements for the labeling widget
     * @returns {undefined}
     */
    _createContainer = function () {
        var labelingContainer = document.createElement('div');
        labelingContainer.id = 'labelingContainer';
        document.body.appendChild(labelingContainer);

        _representation.containerElement = labelingContainer;
        _tileView = window.knimeTileView;
        _tileView.init(_representation, _value);

        if (_representation.useProgressBar) {
            var progressText = document.createElement('div');
            progressText.id = 'progressText';

            var currentProgress = document.createElement('div');
            currentProgress.id = 'labCurrentProgress';

            var currentProgressbar = document.createElement('div');
            currentProgressbar.id = 'labCurrentProgressBar';

            if (!_representation.title || _representation.title === '') {
                currentProgress.className = 'labCurrentProgressWithoutTitle';
            }

            currentProgress.appendChild(currentProgressbar);
            var dataTableWrapper = document.getElementsByClassName('dataTables_wrapper')[0];
            dataTableWrapper.parentNode.insertBefore(currentProgress, dataTableWrapper);
            dataTableWrapper.parentNode.insertBefore(progressText, dataTableWrapper);
            // Initialize progress text
            document.getElementById('progressText').innerHTML = `${_filterData(_getAllPossibleValues()).length} / ${
                _tileView._representation.table.rows.length} processed`;
        } else {
            // If there is no progress bar, there is no need to save more space towards the top
            document.getElementById('knime-service-header').style.paddingTop = '7px';
        }

        var labelingButtonGroup = document.createElement('div');
        labelingButtonGroup.id = 'labelingButtonGroup';
        labelingButtonGroup.className = 'row';
        labelingContainer.appendChild(labelingButtonGroup);

        var infoContainer = document.createElement('div');
        infoContainer.className = 'infoContainer';

        var skipButtonContainer = document.createElement('div');
        skipButtonContainer.id = 'skipButtonContainer';
        var skipButton = document.createElement('button');
        skipButton.type = 'button';
        skipButton.id = 'btnSkip';
        skipButton.value = 'Skip';
        skipButton.innerHTML = 'Skip';
        skipButton.className = 'knime-qf-button btnLabel';
        skipButton.style.visibility = 'hidden';
        skipButtonContainer.appendChild(skipButton);
        infoContainer.appendChild(skipButtonContainer);

        var selectedText = document.createElement('span');
        selectedText.id = 'selectedText';
        selectedText.textContent = 'Selected Tiles: 0';
        infoContainer.appendChild(selectedText);
        labelingButtonGroup.appendChild(infoContainer);

        var editDialog = _createClassEditorDialog();
        labelingButtonGroup.appendChild(editDialog);
        var removeDialog = _createRemoveDialog();
        editDialog.appendChild(removeDialog);

        var labelingDoneText = document.createElement('h1');
        labelingDoneText.innerHTML = 'All elements have been labeled.';
        labelingDoneText.style = 'text-align: center; margin-top: 30px;';

        var labelContainer = document.createElement('div');
        labelContainer.className = 'labelContainer';

        var labelingButtons = document.createElement('div');
        var labelingText = document.createElement('span');
        labelingText.textContent = 'Label as:';
        labelContainer.appendChild(labelingText);
        labelingButtons.id = 'divLabels';
        labelingButtons.className = 'row';
        labelingButtons.style = 'margin-left:-5px';
        labelContainer.appendChild(labelingButtons);
        labelingButtonGroup.appendChild(labelContainer);

        _updateLabelClasses();
        _setupSkipButtonHandler(skipButton);
    };

    /**
     * Initializes the skip/remove button to display the correct label and behaves correctly
     * @param {Element} skipButton - the skip button to setup
     * @returns {undefined}
     */
    _setupSkipButtonHandler = function (skipButton) {
        skipButton.onclick = function (e) {
            if (e.target.innerHTML === 'Skip') {
                _labelAndLoadNext(_hexToRgb(_skipHeaderColor), e.target.innerHTML);
            } else {
                for (var selectedTile in knimeTileView._selection) {
                    if (_value.labels[selectedTile]) {
                        delete _value.labels[selectedTile];
                    }
                }
                _labelAndLoadNext(_hexToRgb(_defaultHeaderColor), '');
            }
        };
    };

    /**
     * Binds the arrow left/right key in order to make tile navigation possible with the arrow keys
     * @returns {undefined}
     */
    _bindArrowKeys = function () {
        var leftArrowKeyCode = 37;
        var rightArrowKeyCode = 39;
        document.onkeydown = function (e) {
            switch (e.keyCode) {
            case leftArrowKeyCode:
                _tileView._getJQueryTable().DataTable().page('previous').draw('page');
                break;
            case rightArrowKeyCode:
                _tileView._getJQueryTable().DataTable().page('next').draw('page');
                break;
            }
        };
    };

    /**
     * Filters the DataTable to only consist of entries which have a label
     * @returns {undefined}
     */
    window.knimeTileView._filterLabeledData = function () {
        var concatValues = _getAllPossibleValues();
        _tileView._getJQueryTable().DataTable().column(1).search(`^(?!.*(${concatValues})).*`, true, false).draw();
    };

    /**
     * Removes any previously set filter on the DataTable
     * @returns {undefined}
     */
    window.knimeTileView._resetFilter = function () {
        _tileView._getJQueryTable().DataTable().columns().search('').draw();
    };

    /**
     * Create the dialog to be able to remove labels which have been created during the use of the view
     * @returns {Object} removeDialog - the created remove-dialog
     */
    _createRemoveDialog = function () {
        var removeDialog = document.createElement('dialog');
        removeDialog.id = 'dlgRemove';

        var removeDialogText = document.createElement('div');
        removeDialogText.innerHTML = '';
        removeDialogText.id = 'dlgRemoveText';
        removeDialog.appendChild(removeDialogText);


        var removeDialogButtonKeep = document.createElement('button');
        removeDialogButtonKeep.innerHTML = 'Keep';
        removeDialogButtonKeep.onclick = function () {
            document.getElementById('dlgRemove').close();
        };
        removeDialog.appendChild(removeDialogButtonKeep);

        var removeDialogButtonAccept = document.createElement('button');
        removeDialogButtonAccept.innerHTML = 'Remove';
        removeDialogButtonAccept.id = 'btnRemove';
        removeDialogButtonAccept.onclick = function () {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected).map(option => option.value);
            classes.forEach((className) => {
                var valInd = _value.possiblevalues.indexOf(className);
                if (valInd > -1) {
                    _value.possiblevalues.splice(valInd, 1);
                }
            });
            _updateLabelClasses();
            document.getElementById('btnRemoveLabelClass').hidden = true;
            document.getElementById('dlgRemove').close();
        };
        removeDialog.appendChild(removeDialogButtonAccept);

        return removeDialog;
    };

    /**
     * Create label class editor dialog
     * @returns {Element} - created editor dialog
     */
    _createClassEditorDialog = function () {
        var editDialog = document.createElement('dialog');
        editDialog.id = 'dlgEdit';

        var editHeader = document.createElement('h1');
        editHeader.innerHTML = 'Labels';
        editDialog.appendChild(editHeader);

        var editTextInput = document.createElement('input');
        editTextInput.id = 'tbxNewLabel';
        editTextInput.style = 'width: 50%;';
        editTextInput.type = 'text';
        editDialog.appendChild(editTextInput);

        var editAddButton = document.createElement('button');
        editAddButton.id = 'btnAddNewLabelClass';
        editAddButton.style = 'width: 50%;';
        editAddButton.type = 'button';
        editAddButton.innerHTML = 'Add';
        _setupAddClassesButtonHandler(editAddButton);
        editDialog.appendChild(editAddButton);
        editDialog.appendChild(document.createElement('br'));
        var editList = document.createElement('select');
        editList.id = 'slcClassesEdit';
        editList.style = 'width: 100%;';
        editList.multiple = true;
        editDialog.appendChild(editList);

        editList.onchange = function () {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected);
            if (classes.length > 0) {
                document.getElementById('btnRemoveLabelClass').hidden = false;
            }
        };

        editDialog.appendChild(document.createElement('br'));

        var editInfoText = document.createElement('p');
        editInfoText.innerHTML = 'Labels set in the node dialog cannot be removed';
        editInfoText.style = 'color:grey';
        editDialog.appendChild(editInfoText);

        var removeButton = document.createElement('button');
        removeButton.id = 'btnRemoveLabelClass';
        removeButton.style = 'width: 100%;';
        removeButton.type = 'button';
        removeButton.innerHTML = 'Remove';
        removeButton.hidden = true;

        removeButton.onclick = function () {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected).map(option => option.value);
            var removeDialogText = document.getElementById('dlgRemoveText');
            removeDialogText.innerHTML = `Are you sure you want to delete the selected classes? There are ${
                _filterData(classes.join('|')).length} tiles marked.`;
            document.getElementById('dlgRemove').showModal();
        };

        editDialog.appendChild(removeButton);
        editDialog.appendChild(document.createElement('br'));

        var closeButton = document.createElement('button');
        closeButton.id = 'btnCloseDialog';
        closeButton.style = 'float: right;';
        closeButton.type = 'button';
        closeButton.innerHTML = 'Close';
        closeButton.onclick = function () {
            document.getElementById('dlgEdit').close();
            _updateLabelClasses();
        };
        editDialog.appendChild(closeButton);

        return editDialog;
    };

    /**
     * Initializes the add-button which creates new labels in the view
     * @param {Element} addClassesButton - the add-button to initialize
     * @returns {undefined}
     */
    _setupAddClassesButtonHandler = function (addClassesButton) {
        addClassesButton.onclick = function () {
            var value = document.getElementById('tbxNewLabel').value;
            var oldIndex = _representation.possiblevalues.indexOf(value);
            var newIndex = _value.possiblevalues.indexOf(value);
            if (oldIndex === -1 && newIndex === -1 && value !== '' && value !== '?') {
                _value.possiblevalues.push(value);
                document.getElementById('tbxNewLabel').value = '';
                _updateLabelClasses();
            } else {
                alert('Label could not be added');
            }
        };
    };

    /**
     * Updates all label classes
     * @returns {undefined}
     */
    _updateLabelClasses = function () {
        _updateDialogClasses();

        if (_value.possiblevalues.length < 8) {
            _updateButtonClasses();
        } else {
            _updateDropdownClasses();
        }
    };

    /**
     * Updates the dropdown label classes
     * @returns {undefined}
     */
    _updateDropdownClasses = function () {
        var buttons = document.getElementById('divLabels');
        buttons.innerHTML = '';
        _updateButtonClasses(7);
        var counter = 7;

        var dropdownContainer = document.createElement('span');
        dropdownContainer.id = 'dropdownContainer';
        var selectClasses = document.createElement('select');
        selectClasses.id = 'slcClassesDropdown';
        dropdownContainer.appendChild(selectClasses);
        buttons.appendChild(dropdownContainer);

        for (var k = counter; k < _value.possiblevalues.length; k++) {
            var addedLabelOption = document.createElement('option');
            addedLabelOption.className = 'btnLabel';
            addedLabelOption.value = _value.possiblevalues[k];
            addedLabelOption.innerHTML = _value.possiblevalues[k];
            _getColorValue(k);
            selectClasses.appendChild(addedLabelOption);
        }

        var labelButton = document.createElement('button');
        labelButton.style = 'margin:5px';
        labelButton.type = 'button';
        labelButton.className = 'btnLabel';
        labelButton.value = 'dropDownLabel';
        labelButton.innerHTML = 'Label';
        labelButton.id = 'labelButton';
        labelButton.onclick = function () {
            var dropdown = document.getElementById('slcClassesDropdown');
            var value = dropdown.options[dropdown.selectedIndex].value;
            var color = knimeTileView._representation.colors[value];
            if (!color) {
                color = knimeTileView._value.colors[value];
            }
            _labelAndLoadNext(_hexToRgb(_getHexColor(color)), value);
        };
        dropdownContainer.appendChild(labelButton);
    };

    /**
     * Updates the dialog label classes
     * @returns {undefined}
     */
    _updateDialogClasses = function () {
        var options = document.getElementById('slcClassesEdit');

        options.innerHTML = '';

        for (var j = 0; j < _representation.possiblevalues.length; j++) {
            var labelOption = document.createElement('option');
            labelOption.disabled = true;
            labelOption.value = _representation.possiblevalues[j];
            labelOption.innerHTML = _representation.possiblevalues[j];
            options.appendChild(labelOption);
        }

        for (var k = _representation.possiblevalues.length; k < _value.possiblevalues.length; k++) {
            var addedLabelOption = document.createElement('option');
            addedLabelOption.disabled = false;
            addedLabelOption.value = _value.possiblevalues[k];
            addedLabelOption.innerHTML = _value.possiblevalues[k];
            options.appendChild(addedLabelOption);
        }
    };

    /**
     * Updates the button label classes
     * @param {number} maxAmount - sets the maximum amount of allowed buttons
     * @returns {undefined}
     */
    _updateButtonClasses = function (maxAmount) {
        var buttons = document.getElementById('divLabels');
        buttons.innerHTML = '';
        var textColor, bgColor;
        var counter = 0;
        var maxStringLength = 15;

        for (var k = 0; k < _value.possiblevalues.length; k++) {
            if (counter === maxAmount) {
                break;
            }
            var addedLabelButton = document.createElement('button');
            addedLabelButton.style = 'margin:5px';
            addedLabelButton.style.setProperty('word-break', 'break-all', '');
            addedLabelButton.type = 'button';
            addedLabelButton.className = 'btnLabel';
            addedLabelButton.value = _value.possiblevalues[k];
            if (_value.possiblevalues[k].length < maxStringLength) {
                addedLabelButton.innerHTML = _value.possiblevalues[k];
            } else {
                addedLabelButton.title = _value.possiblevalues[k];
                addedLabelButton.completeName = _value.possiblevalues[k];
                addedLabelButton.innerHTML = `${_value.possiblevalues[k].substr(0, maxStringLength - 1)}...`;

            }
            bgColor = _getColorValue(k);
            addedLabelButton.style.background = _getHexColor(bgColor);

            textColor = { r: 255, g: 255, b: 255 };
            addedLabelButton.style.color = `rgb(${textColor.r}, ${textColor.g}, ${textColor.b})`;
            addedLabelButton.onclick = function (e) {
                if (e.target.completeName) {
                    _labelAndLoadNext(e.target.style.backgroundColor, e.target.completeName);
                } else {
                    _labelAndLoadNext(e.target.style.backgroundColor, e.target.innerHTML);
                }
            };
            buttons.appendChild(addedLabelButton);
            counter++;
        }
    };

    /**
     * Gets the color value defined for the given index
     * @param {number} index - the index of the color
     * @returns {number} bgColor - integer representation of the color at the given index
     */
    _getColorValue = function (index) {
        var bgColor;
        var defaultColor = _skipHeaderColor;
        if (_representation.colorscheme === 'None') {
            bgColor = defaultColor;
        } else if (index > 6) {
            // if dropdown is used, then colors should all be the same (6 is dropdown limit at the moment)
            // TODO make dropdown limit be configurable
            bgColor = defaultColor;
            _value.colors[_value.possiblevalues[index]] = bgColor;
        } else {
            bgColor = _masterColors[index];
        }
        return bgColor;
    };

    /**
     * Checks if the provided rowId is currently displayed
     * @param {number} rowId - rowId which should be checked if it is displayed
     * @returns {Element | undefined} displayedCheckbox - the checkbox if it is currently visible, undefined otherwise
     */
    _checkIfIsCurrentlyDisplayed = function (rowId) {
        var currentCheckboxes = knimeTileView._curCells[0].parentElement.parentElement
            .getElementsByClassName('selection-cell');
        var displayedCheckbox;
        for (var checkbox in currentCheckboxes) {
            if (checkbox === 'length') {
                break;
            }
            if (currentCheckboxes[checkbox].children[0].value === rowId) {
                displayedCheckbox = currentCheckboxes[checkbox];
                break;
            }
        }
        return displayedCheckbox;
    };

    /**
     * Initializes all the Labels when the view is opened for the first time or redrawn
     * @param {Object} labels - Object which contains all the labels
     * @param {Object} colorMap - Object which stores the color for all the rows
     * @param {Array} rowKeyIndexObj - Array of all the possible rowKeys
     * @param {boolean} redraw - true if the view should be redrawn instead of initialized
     * @returns {Element | undefined} displayedCheckbox - the checkbox if it is currently visible, undefined otherwise
     */
    _initializeLabels = function (labels, colorMap, rowKeyIndexObj, redraw) {
        var tileViewData = _tileView._dataTable.data();
        var lastRowLabeled = null;
        var totalLabels = 0;
        var lastLabeledRowInd = 0;
        _rowKeysOnly.forEach((rowKey) => {
            var label = labels[rowKey];
            var rowInd = rowKeyIndexObj[rowKey].originalRowKey;
            if (!tileViewData[rowInd] ||
                !knimeTileView._knimeTable.isRowIncludedInFilter(rowKey, knimeTileView._currentFilter)) {
                return;
            }
            if (typeof label !== 'undefined' && label !== '?') {
                var color = colorMap[label];
                var labelColor = `rgb(${color.r}, ${color.g}, ${color.b})`;
                tileViewData[rowInd][1] = tileViewData[rowInd][1].replace(
                    /background-color:\s*#[A-Fa-f0-9]{6};*/, `background-color: ${labelColor};`
                );
                tileViewData[rowInd][1] = tileViewData[rowInd][1].replace(
                    /background-color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, `background-color: ${labelColor};`
                );
                if (/"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/.test(tileViewData[rowInd][1])) {
                    tileViewData[rowInd][1] = tileViewData[rowInd][1].replace(
                        /"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, '"color: rgb(255,255,255);'
                    );
                } else {
                    tileViewData[rowInd][1] = tileViewData[rowInd][1].replace(
                        /style="/, 'style="color: rgb(255,255,255);'
                    );
                }
                tileViewData[rowInd][1] = tileViewData[rowInd][1].replace(
                    />.*<\/div>/, `>${label}</div>`
                );
                _tileView._dataTable.row(rowInd).invalidate();
                if (rowInd >= lastLabeledRowInd) {
                    lastRowLabeled = rowKey;
                    lastLabeledRowInd = rowInd;
                }
                totalLabels++;
            }
        });

        if (_value.showUnlabeledOnly) {
            _tileView._filterLabeledData();
        }

        if (totalLabels === tileViewData.length) {
            totalLabels = -1;
        }
        // Calculate the percentage of already labeled tiles and display it
        var tableLength = _tileView._representation.table.rows.length;
        var numberOfPossibleValues = _getTotalNumberLabeled();
        var progress = 1 - (tableLength - numberOfPossibleValues) / tableLength;
        var amountTrueSelectedValues = _countTrueSelectedValues(_tileView._selection);
        if (_tileView._representation.useProgressBar) {
            document.getElementById('labCurrentProgressBar').style.width = `${progress * 100}%`;
            document.getElementById('progressText').innerHTML = `${numberOfPossibleValues} / ${tableLength} processed`;
        }
        if (!redraw) {
            if (_tileView._value.autoSelectNextTile && lastRowLabeled !== '') {
                if (_value.hideUnselected) {
                    // nothing todo as no tile should be selected
                } else if (amountTrueSelectedValues === 0) {
                    var selectedRowName = _selectNextTile([_rowKeysOnly[lastLabeledRowInd]], true);
                    if (selectedRowName) {
                        knimeTileView._selection[selectedRowName] = true;
                    }
                } else {
                    var savedPage = _tileView._value.currentPage;
                    _tileView._getJQueryTable().DataTable().page(savedPage).draw('page');
                }
            }
        }
        _changeSkipButton();
        // need to recalculate as it might have changed
        amountTrueSelectedValues = _countTrueSelectedValues(_tileView._selection);
        // check for amount of selected tiles
        if (amountTrueSelectedValues) {
            _setSelectedTilesText(amountTrueSelectedValues);
        } else {
            // set to 0 if selected is undefined (results from fresh loading)
            _setSelectedTilesText(0);
        }
    };


    /**
     * Initializes all the Labels when the view is opened for the first time or redrawn
     * @param {String} labelColor - the color of the label
     * @param {Object} labelText - the text of the label
     * @returns {undefined} displayedCheckbox - the checkbox if it is currently visible, undefined otherwise
     */
    _labelAndLoadNext = function (labelColor, labelText) {
        var selectedRows = [];
        var rowName;
        // get only the rows which are actually selected
        var selectedKeyValPairs = Object.keys(knimeTileView._selection)
            .filter(key => knimeTileView._selection[key]);
        if (selectedKeyValPairs.length > 0) {
            for (var i = selectedKeyValPairs.length - 1; i >= 0; i--) {
                if (selectedKeyValPairs[i]) {
                    rowName = selectedKeyValPairs[i];
                    if (!knimeTileView._knimeTable.isRowIncludedInFilter(rowName, knimeTileView._currentFilter)) {
                        continue;
                    }
                    _tileView._selection[rowName] = false;
                    // Remove operation
                    var currentTile = _checkIfIsCurrentlyDisplayed(rowName);
                    if (currentTile) {
                        if (labelText === '') {
                            // click the selected Tile
                            currentTile.parentNode.parentNode.labeled = false;
                            $(currentTile.children[0]).click();
                        } else {
                            currentTile.parentNode.parentNode.labeled = true;
                            $(currentTile.children[0]).click();
                        }
                    }
                    selectedRows.push(rowName);
                    delete knimeTileView._selection[rowName];
                }
            }
            var rgbObject;

            // Transform from object to rgb string
            if (typeof labelColor === 'object') {
                rgbObject = labelColor;
                labelColor = `rgb(${rgbObject.r}, ${rgbObject.g}, ${rgbObject.b})`;
            } else {
                var rgbSplit = labelColor.split('(')[1].substring(0, labelColor.split('(')[1].length - 1).split(',');
                rgbObject = {
                    r: parseInt(rgbSplit[0], 10),
                    g: parseInt(rgbSplit[1], 10),
                    b: parseInt(rgbSplit[2], 10)
                };
            }
            var invertedColor = {
                r: 255,
                g: 255,
                b: 255
            };
            var invertedRGBColor = `rgb(${invertedColor.r}, ${invertedColor.g}, ${invertedColor.b})`;

            _tileViewData = _tileView._dataTable.data();
            selectedRows.forEach((row) => {
                if (row !== '?') {
                    if (!_rowKeys[row]) {
                        return;
                    }
                    var rowNumber = _rowKeys[row].originalRowKey;
                    // Check for hex string
                    _tileViewData[rowNumber][1] = _tileViewData[rowNumber][1].replace(
                        /background-color:\s*#[A-Fa-f0-9]{6};*/, `background-color: ${labelColor};`
                    );
                    // Check for rgb background string
                    _tileViewData[rowNumber][1] = _tileViewData[rowNumber][1].replace(
                        /background-color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, `background-color: ${labelColor};`
                    );
                    // Check if text color value is set and if yes replace it
                    if (/"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/.test(_tileViewData[rowNumber][1])) {
                        _tileViewData[rowNumber][1] = _tileViewData[rowNumber][1].replace(
                            /"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, `"color: ${invertedRGBColor};`
                        );
                    } else {
                        _tileViewData[rowNumber][1] = _tileViewData[rowNumber][1].replace(
                            /style="/, `style="color: ${invertedRGBColor};`
                        );
                    }
                    // Replace the text of the tile to the new label
                    _tileViewData[rowNumber][1] = _tileViewData[rowNumber][1].replace(
                        />.*<\/div>/, `>${labelText}</div>`
                    );
                    _tileView._dataTable.row(rowNumber).invalidate();
                    // Save the current label into the view value, if it is not empty.
                    if (labelText !== '') {
                        // vvv new implementation to be fully converted later
                        _rowKeys[row].label = labelText;
                        _value.labels[row] = labelText;
                    }
                }
            });
            _value.selection = {};
            knimeTileView._selection = {};
            if (_value.showUnlabeledOnly) {
                _tileView._filterLabeledData();
            }
            // Calculate the percentage of already labeled tiles and display it
            var tableLength = _tileView._representation.table.rows.length;
            var numberOfPossibleValues = _getTotalNumberLabeled();
            var progress = 1 - (tableLength - numberOfPossibleValues) / tableLength;
            if (_tileView._representation.useProgressBar) {
                document.getElementById('labCurrentProgressBar').style.width = `${progress * 100}%`;
                document.getElementById('progressText').innerHTML = `${numberOfPossibleValues} / ${tableLength} labeled`;
            }
            if (_tileView._value.autoSelectNextTile) {
                var selectedRowName;
                if (_value.hideUnselected) {
                    // nothing todo as no tile should be selected
                } else {
                    knimeService.setSelectedRows(_representation.table.id, [], false);
                    selectedRowName = _selectNextTile(selectedRows, false);
                    // Only set the selected Row if showUnlabeledOnly is false.
                    // This is a dirty fix as the filter screws the row index and so select next would not work properly.
                    // TODO correct this screw (AP-13973)
                    if (selectedRowName && !_value.showUnlabeledOnly) {
                        knimeTileView._selection[selectedRowName] = true;
                    }
                }
            }

            var amountTrueSelectedValues = _countTrueSelectedValues(knimeTileView._selection);
            // check for amount of selected tiles
            if (amountTrueSelectedValues) {
                _setSelectedTilesText(amountTrueSelectedValues);
            } else {
                // set to 0 if selected is undefined (results from fresh loading)
                _setSelectedTilesText(0);
            }
        }
    };

    /**
     * Counts the truly selected tiles
     * Method which is required because the base table viewer sets the boolean of the object entry to false rather then deleting it.
     * TODO check in base table viewer if that makes sense or deleting the row would be more efficient.
     * @param {Object} potentiallySelectedList - An Object that contains all rows which might be selected in format rowKey: boolean
     * @returns {number} - returns the amount of truly selected tiles
     */
    _countTrueSelectedValues = function (potentiallySelectedList) {
        var counter = Object.keys(potentiallySelectedList).reduce(
            (count, listEntry) => potentiallySelectedList[listEntry] &&
             knimeTileView._knimeTable.isRowIncludedInFilter(listEntry, knimeTileView._currentFilter)
                ? count + 1
                : count, 0
        );
        return counter;
    };

    /**
     * Sets the counter text for the amount of selected tiles
     * @param {number} amount - the amount of selectedTiles
     * @returns {undefined}
     */
    _setSelectedTilesText = function (amount) {
        document.getElementById('selectedText').innerHTML = `Selected tiles: ${amount}`;
    };

    /**
     * Gets the amount of total labeled tiles
     * @returns {number} - amount of total labeled tiles
     */
    _getTotalNumberLabeled = function () {
        var possibleLabels = _getAllPossibleValues().split('|');
        var count = 0;
        Object.keys(_value.labels).forEach((rowKey) => {
            if (possibleLabels.includes(_value.labels[rowKey])) {
                count++;
            }
        });
        return count;
    };

    /**
     * Selects the next tile
     * @param {Object} selectedRows - Object containing all selected rows in format {id: true/false}
     * @param {boolean} initialize - if true initialize the view
     * @returns {String|boolean} - next row key to select or false if there is no next row key
     */
    _selectNextTile = function (selectedRows, initialize) {
        var currentPageCells = knimeTileView._curCells[0];
        var prevRowInd = 0;
        var info = _tileView._getJQueryTable().DataTable().page.info();
        var pageSize = info.length;
        if (!currentPageCells) {
            return false;
        }
        var currentCheckboxes = currentPageCells.parentElement.parentElement
            .getElementsByClassName('selection-cell');
        selectedRows.forEach((row) => {
            if (!_rowKeys[row]) {
                return;
            }
            var rowInd = _rowKeys[row].rowInd;
            if (prevRowInd < rowInd) {
                prevRowInd = rowInd;
            }
        });

        // Check if previous index was last, then it should select first tile instead of the next
        var newRowInd = prevRowInd >= info.recordsDisplay - 1 || initialize
            ? 0
            : _rowKeys[_rowKeysOnly[prevRowInd + 1]].rowInd;
        // Always choose the first row when showLabeledOnly is active.
        if (_value.showUnlabeledOnly) {
            newRowInd = 0;
        }
        var currentPage = _tileView._dataTable.page();
        var savedPage = _tileView._value.currentPage;
        var pageForRow = currentPage !== savedPage && initialize ? savedPage : Math.floor(newRowInd / pageSize);

        if (pageForRow >= info.pages) {
            pageForRow = 0;
        }

        if (currentPage !== pageForRow && !_value.showUnlabeledOnly) {
            _tileView._getJQueryTable().DataTable().page(pageForRow).draw('page');
            _tileView._value.currentPage = pageForRow;
        }

        // Check if its not the initial call, if so then a new tile needs to be selected or if it is a initial call and there is no different saved page from a previous view, then also select the next tile
        if (!initialize || (initialize && currentPage === savedPage)) {
            var checkboxIndex = newRowInd - pageForRow * pageSize;
            $(currentCheckboxes[checkboxIndex]).click();
            // Otherwise set the saved page to the correct one and stop
        } else {
            _tileView._value.currentPage = pageForRow;
            return false;
        }

        var newRowKey = _rowKeysOnly[newRowInd];
        return newRowKey;
    };


    /**
     * Function that prepares a string with all possible labels concatenated
     * @returns {String} - a string consisting of all possible values concatenated and seperated by a '|'.
     *                     Adittionally the string for the skip label is always added.
     */
    _getAllPossibleValues = function () {
        var concatValues = 'Skip|';
        var possibleValue, possibleNewValues;
        for (possibleValue in _tileView._representation.possiblevalues) {
            concatValues += `${_representation.possiblevalues[possibleValue]} |`;
        }
        concatValues = concatValues.slice(0, -1);
        if (_tileView._value.possiblevalues.length > 0) {
            for (possibleNewValues in _tileView._value.possiblevalues) {
                concatValues += `|${_value.possiblevalues[possibleNewValues]}`;
            }
        } else {
            concatValues = concatValues.slice(0, concatValues.length - 1);
        }
        return concatValues;
    };


    /**
     * Filter the DataTable acording to a provided search term
     * @param {String} searchTerm - a string containing all search terms in format 'Term1|Term2|Term3...
     * @returns {Object} - DataTable object containing only values which include the search term
     */
    _filterData = function (searchTerm) {
        // TODO: improve the performance of this function
        var searchTerms = searchTerm.split('|');
        return _tileView._getJQueryTable().DataTable().column(1).data().filter((value) => {
            var temp1 = value.split('>');
            var temp2 = temp1.length && temp1.length >= 2 ? temp1[1].split('<')[0] : '';
            return searchTerms.includes(temp2[0]);
        });
    };

    /**
     * Checks the selected tiles and changes the style and text of the skip/remove button
     * @returns {undefined}
     */
    _changeSkipButton = function () {
        var skipButton = document.getElementById('btnSkip');
        if (_countTrueSelectedValues(knimeTileView._selection) > 0) {
            skipButton.style.visibility = 'visible';
        } else {
            skipButton.style.visibility = 'hidden';
        }

        var onlyUnlabeled = true;
        for (var selectedTile in knimeTileView._selection) {
            if (knimeTileView._value.labels[selectedTile] &&
                knimeTileView._value.labels[selectedTile] !== '?' &&
                knimeTileView._selection[selectedTile]) {
                onlyUnlabeled = false;
            }
        }
        if (onlyUnlabeled) {
            skipButton.innerHTML = 'Skip';
        } else {
            skipButton.innerHTML = 'Remove Label';
        }
    };

    /**
     * Returns the hex representation of the input color integer integer
     * @param {int} number - an integer representation of a color one wants to have in hex-color format
     * @returns {String} - the hexcode-color representation of the provided integer
     */
    _getHexColor = function (number) {
        var hexCode = (number >>> 0).toString(16).slice(-6);
        while (hexCode.length < 6) {
            hexCode = `0${hexCode}`;
        }
        hexCode = `#${hexCode}`;
        return hexCode;
    };

    /**
     * Checks the selected tiles and changes the style and text of the skip/remove button
     * @param {String} hex - a string containing a hex-color representation
     * @returns {Object | null} - either returns an object containing the three rgb channels of the provided hex color or null
     */
    _hexToRgb = function (hex) {
        var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result
            ? {
                r: parseInt(result[1], 16),
                g: parseInt(result[2], 16),
                b: parseInt(result[3], 16)
            }
            : null;
    };

    return labelingWidget;
})();
