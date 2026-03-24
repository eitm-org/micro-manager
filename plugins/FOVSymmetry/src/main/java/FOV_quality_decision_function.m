function decision = FOV_quality_decision_function(modelMatPath, imageInput)
% predictFOVCombinedFromModel
%
% Predict whether a single image is a good or bad FOV using a saved
% combined detector model exported from the training/tuning code.
%
% INPUTS:
%   modelMatPath : path to .mat file containing deployedModel
%
%   imageInput   : either
%                  1) image file path
%                  2) image matrix already loaded in MATLAB
%
% OUTPUT:
%   decision     : 'g' for good FOV, 'b' for bad FOV
%
% EXAMPLES:
%   decision = FOV_quality_decision( ...
%       "Z:\...\combined_FOV_detector_model_20260319_143500.mat", ...
%       "Z:\...\someImage.tif");
%
%   I = imread("Z:\...\someImage.tif");
%   decision = FOV_quality_decision(modelPath, I);

    %% ================= LOAD MODEL =================
    if ~(ischar(modelMatPath) || isstring(modelMatPath))
        error('modelMatPath must be a file path.');
    end

    modelMatPath = string(modelMatPath);

    if ~isfile(modelMatPath)
        error('Model file not found:\n%s', modelMatPath);
    end

    S = load(modelMatPath);

    if ~isfield(S, 'deployedModel')
        error('The .mat file does not contain a variable named deployedModel.');
    end

    model = S.deployedModel;

    requiredFields = { ...
        'thrComb', ...
        'weights', ...
        'bias', ...
        'muX', ...
        'sigmaX', ...
        'nTileRows', ...
        'nTileCols', ...
        'modeStr'};

    for k = 1:numel(requiredFields)
        if ~isfield(model, requiredFields{k})
            error('The model is missing required field: %s', requiredFields{k});
        end
    end

    thrComb   = double(model.thrComb);
    weights   = double(model.weights(:));
    bias      = double(model.bias);
    muX       = double(model.muX(:).');
    sigmaX    = double(model.sigmaX(:).');
    nTileRows = double(model.nTileRows);
    nTileCols = double(model.nTileCols);
    modeStr   = char(model.modeStr);

    expectedNumFeatures = 2 + nTileRows * nTileCols;

    if numel(weights) ~= expectedNumFeatures
        error('Model weights length (%d) does not match expected number of features (%d).', ...
            numel(weights), expectedNumFeatures);
    end

    if numel(muX) ~= expectedNumFeatures
        error('Model muX length (%d) does not match expected number of features (%d).', ...
            numel(muX), expectedNumFeatures);
    end

    if numel(sigmaX) ~= expectedNumFeatures
        error('Model sigmaX length (%d) does not match expected number of features (%d).', ...
            numel(sigmaX), expectedNumFeatures);
    end

    sigmaX(~isfinite(sigmaX) | sigmaX == 0) = 1;

    %% ================= LOAD IMAGE =================
    if ischar(imageInput) || isstring(imageInput)
        imagePath = string(imageInput);

        if ~isfile(imagePath)
            error('Image file not found:\n%s', imagePath);
        end

        I = imread(imagePath);
    else
        I = imageInput;
    end

    %% ================= EXTRACT FEATURES =================
    feat = extractSelectedFeatures(I, nTileRows, nTileCols);

    % Must match training exactly:
    % Xraw = [lrVals, gradMagVals, tileMat];
    % tileMat row was built from feat.tileMap(:).'
    Xraw = [feat.lrSharpImbalance, feat.gradMagCV, feat.tileMap(:).'];

    if numel(Xraw) ~= expectedNumFeatures
        error('Extracted feature vector length (%d) does not match expected (%d).', ...
            numel(Xraw), expectedNumFeatures);
    end

    %% ================= STANDARDIZE USING TRAINING muX/sigmaX =================
    Xz = (Xraw - muX) ./ sigmaX;

    %% ================= COMPUTE COMBINED SCORE =================
    combinedScore = Xz * weights + bias;

    %% ================= APPLY TRAINED DECISION RULE =================
    decision = applyThresholdGeneral(combinedScore, thrComb, modeStr);
    decision = char(decision);
end

%% ================= HELPER FUNCTIONS =================

function feat = extractSelectedFeatures(I, nTileRows, nTileCols)
    I = convertToGrayDouble01(I);

    sob = fspecial('sobel');
    Gx = imfilter(I, sob', 'replicate');
    Gy = imfilter(I, sob,  'replicate');
    Gmag = sqrt(Gx.^2 + Gy.^2);

    [H, W] = size(I);
    tileMap = nan(nTileRows, nTileCols);

    rEdges = round(linspace(1, H+1, nTileRows+1));
    cEdges = round(linspace(1, W+1, nTileCols+1));

    for rr = 1:nTileRows
        for cc = 1:nTileCols
            r1 = rEdges(rr);
            r2 = rEdges(rr+1)-1;
            c1 = cEdges(cc);
            c2 = cEdges(cc+1)-1;

            if r2 < r1 || c2 < c1
                continue;
            end

            tileMag = Gmag(r1:r2, c1:c2);
            tileMap(rr,cc) = mean(tileMag(:));
        end
    end

    gradMagCV = std(tileMap(:), 0, 'omitnan') / max(mean(tileMap(:), 'omitnan'), eps);

    midC = nTileCols / 2;
    leftMean  = mean(tileMap(:,1:midC), 'all', 'omitnan');
    rightMean = mean(tileMap(:,midC+1:end), 'all', 'omitnan');
    lrSharpImbalance = abs(leftMean - rightMean) / max(leftMean + rightMean, eps);

    muTile = mean(tileMap(:), 'omitnan');
    if isfinite(muTile) && muTile > 0
        tileMapNorm = tileMap / muTile;
    else
        tileMapNorm = zeros(size(tileMap));
    end

    feat.lrSharpImbalance = lrSharpImbalance;
    feat.gradMagCV        = gradMagCV;
    feat.tileMap          = tileMapNorm;
end

function pred = applyThresholdGeneral(vals, thr, modeStr)
    vals = vals(:);
    pred = strings(size(vals));

    switch lower(modeStr)
        case 'less_is_good'
            pred(vals <= thr) = "g";
            pred(vals >  thr) = "b";
        case 'greater_is_good'
            pred(vals >= thr) = "g";
            pred(vals <  thr) = "b";
        otherwise
            error('Unknown modeStr: %s', modeStr);
    end
end

function I = convertToGrayDouble01(I)
    if ndims(I) == 3
        I = rgb2gray(I);
    end

    I = double(I);
    minI = min(I(:));
    maxI = max(I(:));

    if maxI > minI
        I = (I - minI) / (maxI - minI);
    else
        I = zeros(size(I));
    end
end